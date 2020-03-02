package com.creactiviti.piper.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * 
 * @author Arik Cohen
 * @since Mar, 02 2020
 */
@Configuration
@EnableWebMvc
public class WebAppConfiguration implements WebMvcConfigurer {

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer aConfigurer) {
    aConfigurer.defaultContentType(MediaType.APPLICATION_JSON)
               .ignoreAcceptHeader(true);
  }

}
