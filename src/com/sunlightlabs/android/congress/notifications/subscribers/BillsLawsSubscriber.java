package com.sunlightlabs.android.congress.notifications.subscribers;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.fragments.BillListFragment;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class BillsLawsSubscriber extends Subscriber {
	
	@Override
	public String decodeId(Object result) {
		return ((Bill) result).id;
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		Utils.setupRTC(context);
		
		try {
			return BillService.recentLaws(1, BillListFragment.PER_PAGE);
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest bills for " + subscription, e);
			return null;
		}
	}
	
	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results == BillListFragment.PER_PAGE)
			return results + " or more new bills signed into law.";
		else if (results > 1)
			return results + " new bills signed into law.";
		else
			return results + " new bill signed into law.";
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return new Intent().setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.MenuBills")
			.putExtra("tab", "bills_law");
	}
	
	@Override
	public String subscriptionName(Subscription subscription) {
		return context.getResources().getString(R.string.menu_bills_law_subscription);
	}
}