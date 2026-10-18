/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import java.io.Serial;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class InMemoryTransactionManager extends AbstractPlatformTransactionManager {

    @Serial
    private static final long serialVersionUID = 1L;

    private transient TransactionState current;
    private transient boolean committed;
    private transient boolean rolledBack;

    boolean isCommitted() {
        return committed;
    }

    boolean isRolledBack() {
        return rolledBack;
    }

    @SuppressWarnings("unchecked")
    <T> T transactionalProxy(T target) {
        MatchAlwaysTransactionAttributeSource transactionAttributeSource = new MatchAlwaysTransactionAttributeSource();

        transactionAttributeSource.setTransactionAttribute(new DefaultTransactionAttribute());

        ProxyFactory proxyFactory = new ProxyFactory(target);

        proxyFactory.addAdvice(new TransactionInterceptor(this, transactionAttributeSource));

        return (T) proxyFactory.getProxy();
    }

    @Override
    protected Object doGetTransaction() {
        return new TransactionObject(current);
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) {
        return ((TransactionObject) transaction).state != null;
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        TransactionState state = new TransactionState();

        ((TransactionObject) transaction).state = state;
        current = state;
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        committed = true;
        current = null;
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        rolledBack = true;
        current = null;
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        ((TransactionObject) status.getTransaction()).state.rollbackOnly = true;
    }

    private static final class TransactionState {

        private boolean rollbackOnly;
    }

    private static final class TransactionObject implements SmartTransactionObject {

        private TransactionState state;

        private TransactionObject(TransactionState state) {
            this.state = state;
        }

        @Override
        public boolean isRollbackOnly() {
            return state != null && state.rollbackOnly;
        }
    }
}
