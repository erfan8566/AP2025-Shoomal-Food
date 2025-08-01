package com.aut.shoomal.dao;

import com.aut.shoomal.payment.transaction.PaymentTransaction;
import com.aut.shoomal.payment.PaymentMethod;
import com.aut.shoomal.payment.transaction.PaymentTransactionStatus;
import org.hibernate.Session;

import java.util.List;

public interface TransactionDao extends GenericDao<PaymentTransaction>
{
    List<PaymentTransaction> findByUserId(Long userId);
    PaymentTransaction findByOrderId(Session session, Integer orderId);
    List<PaymentTransaction> findAllWithFilters(String search, Long userId, PaymentMethod method, PaymentTransactionStatus status);
}