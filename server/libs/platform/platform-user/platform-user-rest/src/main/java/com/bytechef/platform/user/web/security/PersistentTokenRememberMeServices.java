/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.user.web.security;

import com.bytechef.platform.security.config.SecurityProperties;
import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.PersistentTokenService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.util.RandomUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Custom implementation of Spring Security's RememberMeServices.
 * <p>
 * Persistent tokens are used by Spring Security to automatically log in users.
 * <p>
 * This is a specific implementation of Spring Security's remember-me authentication, but it is much more powerful than
 * the standard implementations:
 * <ul>
 * <li>It allows a user to see the list of his currently opened sessions, and invalidate them</li>
 * <li>It stores more information, such as the IP address and the user agent, for audit purposes</li>
 * <li>When a user logs out, only his current session is invalidated, and not all of his sessions</li>
 * </ul>
 * <p>
 * Please note that it allows the use of the same token for 5 seconds, and this value stored in a specific cache during
 * that period. This is to allow concurrent requests from the same user: otherwise, two requests being sent at the same
 * time could invalidate each other's token.
 * <p>
 * This is inspired by:
 * <ul>
 * <li><a href="https://github.com/blog/1661-modeling-your-app-s-user-session">GitHub's "Modeling your App's User
 * Session"</a></li>
 * </ul>
 * <p>
 * The main algorithm comes from Spring Security's {@code PersistentTokenBasedRememberMeServices}, but this class
 * couldn't be cleanly extended.
 *
 * @author Ivica Cardic
 */
@Service
public class PersistentTokenRememberMeServices extends AbstractRememberMeServices implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(PersistentTokenRememberMeServices.class);

    // Token is valid for one month
    private static final int TOKEN_VALIDITY_DAYS = 31;
    private static final int TOKEN_VALIDITY_SECONDS = 60 * 60 * 24 * TOKEN_VALIDITY_DAYS;
    private static final long UPGRADED_TOKEN_VALIDITY_MILLIS = 5000L;

    private ApplicationContext applicationContext;
    private final PersistentTokenCache<UpgradedRememberMeToken> upgradedTokenCache;
    private final PersistentTokenService persistentTokenService;

    @SuppressFBWarnings("EI")
    public PersistentTokenRememberMeServices(
        SecurityProperties securityProperties,
        org.springframework.security.core.userdetails.UserDetailsService userDetailsService,
        PersistentTokenService persistentTokenService) {

        super(getKey(securityProperties), userDetailsService);

        this.persistentTokenService = persistentTokenService;
        this.upgradedTokenCache = new PersistentTokenCache<>(UPGRADED_TOKEN_VALIDITY_MILLIS);
    }

    @Override
    protected UserDetails processAutoLoginCookie(
        String[] cookieTokens, HttpServletRequest request, HttpServletResponse response) {

        synchronized (this) { // prevent 2 authentication requests from the same user in parallel
            String login = null;
            UpgradedRememberMeToken upgradedToken = upgradedTokenCache.get(cookieTokens[0]);

            if (upgradedToken != null) {
                login = upgradedToken.getUserLoginIfValid(cookieTokens);

                logger.debug("Detected previously upgraded login token for user '{}'", login);
            }

            if (login == null) {
                PersistentToken token = getPersistentToken(cookieTokens);

                UserService userService = applicationContext.getBean(UserService.class);

                User user = userService.getUser(token.getUserId());

                login = user.getLogin();

                // Token also matches, so login is valid. Update the token value, keeping the *same* series number.
                logger.debug("Refreshing persistent login token for user '{}', series '{}'", login, token.getSeries());

                token.setTokenDate(LocalDate.now());
                token.setTokenValue(RandomUtils.generateRandomAlphanumericString());
                token.setIpAddress(request.getRemoteAddr());
                token.setUserAgent(request.getHeader("User-Agent"));

                try {
                    persistentTokenService.save(token);
                } catch (DataAccessException e) {
                    logger.error("Failed to update token: ", e);

                    throw new RememberMeAuthenticationException("Autologin failed due to data access problem", e);
                }

                addCookie(token, request, response);
                upgradedTokenCache.put(cookieTokens[0], new UpgradedRememberMeToken(cookieTokens, login));
            }

            return getUserDetailsService().loadUserByUsername(login);
        }
    }

    @Override
    protected void onLoginSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {
        String login = successfulAuthentication.getName();

        logger.debug("Creating new persistent login for user {}", login);

        UserService userService = applicationContext.getBean(UserService.class);

        PersistentToken token = userService.fetchUserByLogin(login)
            .map(u -> {
                PersistentToken t = new PersistentToken();

                t.setNew(true);
                t.setSeries(RandomUtils.generateRandomAlphanumericString());
                t.setUser(u);
                t.setTokenValue(RandomUtils.generateRandomAlphanumericString());
                t.setTokenDate(LocalDate.now());
                t.setIpAddress(request.getRemoteAddr());
                t.setUserAgent(request.getHeader("User-Agent"));

                return t;
            })
            .orElseThrow(() -> new UsernameNotFoundException("User " + login + " was not found in the database"));

        try {
            persistentTokenService.save(token);

            addCookie(token, request, response);
        } catch (DataAccessException e) {
            logger.error("Failed to save persistent token ", e);
        }
    }

    /**
     * When logout occurs, only invalidate the current token, and not all user sessions.
     * <p>
     * The standard Spring Security implementations are too basic: they invalidate all tokens for the current user, so
     * when he logs out from one browser, all his other sessions are destroyed.
     *
     * @param request        the request.
     * @param response       the response.
     * @param authentication the authentication.
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String rememberMeCookie = extractRememberMeCookie(request);

        if (rememberMeCookie != null && rememberMeCookie.length() != 0) {
            try {
                String[] cookieTokens = decodeCookie(rememberMeCookie);
                PersistentToken token = getPersistentToken(cookieTokens);
                persistentTokenService.delete(token.getSeries());
            } catch (InvalidCookieException ice) {
                logger.info("Invalid cookie, no persistent token could be deleted", ice);
            } catch (RememberMeAuthenticationException rmae) {
                logger.debug("No persistent token found, so no token could be deleted", rmae);
            }
        }

        super.logout(request, response, authentication);
    }

    /**
     * Validate the token and return it.
     */
    private PersistentToken getPersistentToken(String[] cookieTokens) {
        if (cookieTokens.length != 2) {
            throw new InvalidCookieException(
                "Cookie token did not contain " + 2 + " tokens, but contained '" + Arrays.asList(cookieTokens) + "'");
        }

        String presentedSeries = cookieTokens[0];
        String presentedToken = cookieTokens[1];

        Optional<PersistentToken> optionalToken = persistentTokenService.fetchPersistentToken(presentedSeries);

        if (!optionalToken.isPresent()) {
            // No series match, so we can't authenticate using this cookie
            throw new RememberMeAuthenticationException("No persistent token found for series id: " + presentedSeries);
        }

        PersistentToken token = optionalToken.orElseThrow();

        // We have a match for this user/series combination
        logger.info("presentedToken={} / tokenValue={}", presentedToken, token.getTokenValue());

        if (!presentedToken.equals(token.getTokenValue())) {
            // Token doesn't match series value. Delete this session and throw an exception.
            persistentTokenService.delete(token.getSeries());

            throw new CookieTheftException(
                "Invalid remember-me token (Series/token) mismatch. Implies previous " + "cookie theft attack.");
        }

        LocalDate tokenDate = token.getTokenDate()
            .plusDays(TOKEN_VALIDITY_DAYS);

        if (tokenDate.isBefore(LocalDate.now())) {
            persistentTokenService.delete(token.getSeries());

            throw new RememberMeAuthenticationException("Remember-me login has expired");
        }

        return token;
    }

    private void addCookie(PersistentToken token, HttpServletRequest request, HttpServletResponse response) {
        setCookie(new String[] {
            token.getSeries(), token.getTokenValue()
        }, TOKEN_VALIDITY_SECONDS, request, response);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private static String getKey(SecurityProperties securityProperties) {
        return securityProperties
            .getRememberMe()
            .getKey();
    }

    private record UpgradedRememberMeToken(String[] upgradedToken, String userLogin) implements Serializable {

        String getUserLoginIfValid(String[] currentToken) {
            if (currentToken[0].equals(this.upgradedToken[0]) && currentToken[1].equals(this.upgradedToken[1])) {
                return this.userLogin;
            }

            return null;
        }
    }
}
