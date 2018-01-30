package io.bisq.core.payment;

import io.bisq.core.offer.Offer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class PaymentAccounts {
    private static final Logger log = LoggerFactory.getLogger(PaymentAccounts.class);

    private final Set<PaymentAccount> accounts;
    private final AccountAgeWitnessService service;

    PaymentAccounts(Set<PaymentAccount> accounts, AccountAgeWitnessService service) {
        this.accounts = accounts;
        this.service = service;
    }

    @Nullable
    PaymentAccount getMostMaturePaymentAccountForOffer(Offer offer) {
        Comparator<PaymentAccount> comparator = this::compareByAge;

        List<PaymentAccount> sortedAccounts = accounts.stream()
                .filter(paymentAccount -> PaymentAccountUtil.isPaymentAccountValidForOffer(offer, paymentAccount))
                .sorted(comparator.reversed())
                .collect(Collectors.toList());

        logValidAccounts(service, sortedAccounts);

        return firstOrNull(sortedAccounts);
    }

    @Nullable
    private PaymentAccount firstOrNull(List<PaymentAccount> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    private static void logValidAccounts(AccountAgeWitnessService service, List<PaymentAccount> accounts) {
        if (log.isDebugEnabled()) {
            StringBuilder message = new StringBuilder("Valid accounts: \n");
            for (PaymentAccount account : accounts) {
                String accountName = account.getAccountName();
                String witnessHex = service.getMyWitnessHashAsHex(account.getPaymentAccountPayload());

                message.append("name = ")
                        .append(accountName)
                        .append("; witness hex = ")
                        .append(witnessHex)
                        .append(";\n");
            }

            log.debug(message.toString());
        }
    }

    private int compareByAge(PaymentAccount left, PaymentAccount right) {
        AccountAgeWitness leftWitness = service.getMyWitness(left.getPaymentAccountPayload());
        AccountAgeWitness rightWitness = service.getMyWitness(right.getPaymentAccountPayload());

        Date now = new Date();
        long leftAge = service.getAccountAge(leftWitness, now);
        long rightAge = service.getAccountAge(rightWitness, now);

        return Long.compare(leftAge, rightAge);
    }
}
