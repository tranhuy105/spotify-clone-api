package com.tranhuy105.musicserviceapi.repository.api;

import com.tranhuy105.musicserviceapi.model.SubscriptionPlan;
import com.tranhuy105.musicserviceapi.model.UserSubscription;
import org.springframework.lang.NonNull;

import java.util.List;

public interface SubscriptionRepository {
    List<UserSubscription> findUserSubscriptions(@NonNull Long id);

    List<SubscriptionPlan> findAllSubscriptionPlan();

    void createSubscriptionPlan(@NonNull Long userId, @NonNull Short planId);
}
