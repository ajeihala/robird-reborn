package com.aaplab.robird.data.model;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.aaplab.robird.Analytics;
import com.aaplab.robird.Config;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by majid on 20.08.15.
 */
public final class BillingModel implements BillingProcessor.IBillingHandler {

    public static final String UNLOCK_IN_APP_BROWSER = "unlock_in_app_browser";
    public static final String SECOND_ACCOUNT_PRODUCT_ID = "second_account";
    public static final String THIRD_ACCOUNT_PRODUCT_ID = "third_account";
    public static final String UNLOCK_ALL_PRODUCT_ID = "unlock_all";
    public static final String UNLOCK_UI_PRODUCT_ID = "unlock_ui";
    public static final String UNLOCK_OTHER_PRODUCT_ID = "unlock_other";

    private BillingProcessor mBillingProcessor;
    private PublishSubject<String> mProductSubject;
    private Activity mActivity;

    public BillingModel(Activity activity) {
        mBillingProcessor = new BillingProcessor(activity, Config.LICENSE_KEY, this);
        mProductSubject = PublishSubject.create();
        mActivity = activity;
    }

    public boolean restorePurchaseHistory() {
        Analytics.event(Analytics.RESTORE);
        return mBillingProcessor.loadOwnedPurchasesFromGoogle();
    }

    public Observable<String> purchase(String productId) {
        mBillingProcessor.purchase(mActivity, productId);
        return mProductSubject;
    }

    public boolean isPurchased(String productId) {
        return mBillingProcessor.isPurchased(UNLOCK_ALL_PRODUCT_ID) || mBillingProcessor.isPurchased(productId);
    }

    public boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        return mBillingProcessor.handleActivityResult(requestCode, resultCode, data);
    }

    public void onDestroy() {
        if (mBillingProcessor != null)
            mBillingProcessor.release();
    }

    @Override
    public void onProductPurchased(String s, TransactionDetails transactionDetails) {
        Analytics.purchase(s);
        mProductSubject.onNext(s);
    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onBillingError(int i, Throwable throwable) {
        mProductSubject.onError(throwable);
    }

    @Override
    public void onBillingInitialized() {

    }
}
