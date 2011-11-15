package com.sunlightlabs.android.congress.fragments;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.BillList;
import com.sunlightlabs.android.congress.CommitteeList;
import com.sunlightlabs.android.congress.R;
import com.sunlightlabs.android.congress.RollList;
import com.sunlightlabs.android.congress.tasks.LoadPhotoTask;
import com.sunlightlabs.android.congress.utils.Analytics;
import com.sunlightlabs.android.congress.utils.FragmentUtils;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.congress.models.Legislator;

public class LegislatorProfileFragment extends ListFragment implements LoadPhotoTask.LoadsPhoto {
	private Legislator legislator;
	
	private Drawable avatar;
	private ViewGroup mainView;
	
	public static LegislatorProfileFragment create(Legislator legislator) {
		LegislatorProfileFragment frag = new LegislatorProfileFragment();
		Bundle args = new Bundle();
		args.putSerializable("legislator", legislator);
		frag.setArguments(args);
		frag.setRetainInstance(true);
		return frag;
	}
	
	public LegislatorProfileFragment() {}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        FragmentUtils.setupSunlight(this);
        
        legislator = (Legislator) getArguments().getSerializable("legislator");
        
        loadPhoto();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list_bare, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setupControls();
		
		if (avatar != null)
			displayAvatar();
	}

	public void loadPhoto() {
		new LoadPhotoTask(this, LegislatorImage.PIC_LARGE).execute(legislator.getId());
	}

	public void onLoadPhoto(Drawable avatar, Object tag) {
		if (avatar == null) {
			Resources resources = null;
			if (getActivity() != null)
				resources = getActivity().getResources();
			
			if (resources != null) {
				if (legislator.gender.equals("M"))
					avatar = resources.getDrawable(R.drawable.no_photo_male);
				else // "F"
					avatar = resources.getDrawable(R.drawable.no_photo_female);
			}
		}
		this.avatar = avatar;
		
		if (isAdded())
			displayAvatar();
	}
	
	public Context getContext() {
		return getActivity();
	}
	
    public void displayAvatar() {
    	((ImageView) mainView.findViewById(R.id.profile_picture)).setImageDrawable(avatar);
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	Object tag = v.getTag();
    	if (tag == null) // not sure how this would happen, but Market error reports indicate it can
    		return;
    
		String type = (String) tag;
    	if (type.equals("phone"))
    		callOffice();
    	else if (type.equals("voting"))
    		votingRecord();
    	else if (type.equals("sponsored"))
    		sponsoredBills();
    	else if (type.equals("committees"))
    		viewCommittees();
    }
    
    public void callOffice() {
    	Analytics.legislatorCall(getActivity(), legislator.id);
    	startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel://" + legislator.phone)));
    }
    
    public void visitWebsite() {
    	Analytics.legislatorWebsite(getActivity(), legislator.id);
    	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(legislator.website)));
    }
    
    public void votingRecord() {
    	startActivity(new Intent(getActivity(), RollList.class)
			.putExtra("type", RollList.ROLLS_VOTER)
			.putExtra("legislator", legislator));
    }
    
    public void sponsoredBills() {
    	startActivity(new Intent(getActivity(), BillList.class)
			.putExtra("type", BillList.BILLS_SPONSOR)
			.putExtra("legislator", legislator));
    }
    
    public void viewCommittees() {
    	startActivity(new Intent(getActivity(), CommitteeList.class)
			.putExtra("legislator", legislator));
    }
    
    public void districtMap() {
		String url = Utils.districtMapUrl(legislator.title, legislator.state, legislator.district);
		Uri uri = Uri.parse("geo:0,0?q=" + url);
		Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri); 
		mapIntent.setData(uri);
		
		Analytics.legislatorDistrict(getActivity(), legislator.id);
		
		startActivity(Intent.createChooser(mapIntent, getString(R.string.view_legislator_district)));
	}


	public void setupControls() {
		MergeAdapter adapter = new MergeAdapter();
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		
		mainView = (ViewGroup) inflater.inflate(R.layout.profile, null);
		
		mainView.setEnabled(false);
		
		if (!legislator.in_office) {
			mainView.findViewById(R.id.out_of_office_text).setVisibility(View.VISIBLE);
			mainView.findViewById(R.id.website).setVisibility(View.GONE);
		}
		
		if (legislator.website == null || legislator.website.equals(""))
			mainView.findViewById(R.id.website).setVisibility(View.GONE);
		
		mainView.findViewById(R.id.website).setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				visitWebsite();
			}
		});
		mainView.findViewById(R.id.district).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				districtMap();
			}
		});
		
		((TextView) mainView.findViewById(R.id.profile_party)).setText(partyName(legislator.party));
		((TextView) mainView.findViewById(R.id.profile_state)).setText(Utils.stateCodeToName(getActivity(), legislator.state));
		((TextView) mainView.findViewById(R.id.profile_domain)).setText(domainName(legislator.getDomain()));
		((TextView) mainView.findViewById(R.id.profile_office)).setText(officeName(legislator.congress_office));
		
		adapter.addView(mainView);
	
		List<View> contactViews = new ArrayList<View>(4);
		
		String phone = legislator.phone;
		if (legislator.in_office && phone != null && !phone.equals("")) {
			View phoneView = inflater.inflate(R.layout.icon_list_item, null);
			((TextView) phoneView.findViewById(R.id.text)).setText("Call " + pronoun(legislator.gender) + " office");
			((ImageView) phoneView.findViewById(R.id.icon)).setImageResource(R.drawable.phone);
			phoneView.setTag("phone");
			contactViews.add(phoneView);
		}
		
		View votingRecordView = inflater.inflate(R.layout.icon_list_item, null);
		((TextView) votingRecordView.findViewById(R.id.text)).setText(R.string.voting_record);
		((ImageView) votingRecordView.findViewById(R.id.icon)).setImageResource(R.drawable.votes);
		votingRecordView.setTag("voting");
		contactViews.add(votingRecordView);
		
		View sponsoredView = inflater.inflate(R.layout.icon_list_item, null);
		((TextView) sponsoredView.findViewById(R.id.text)).setText(R.string.sponsored_bills);
		((ImageView) sponsoredView.findViewById(R.id.icon)).setImageResource(R.drawable.bills);
		sponsoredView.setTag("sponsored");
		contactViews.add(sponsoredView);
		
		View committeesView = inflater.inflate(R.layout.icon_list_item, null);
		((TextView) committeesView.findViewById(R.id.text)).setText(R.string.committees);
		((ImageView) committeesView.findViewById(R.id.icon)).setImageResource(R.drawable.committees);
		committeesView.setTag("committees");
		contactViews.add(committeesView);
		
		adapter.addAdapter(new ViewArrayAdapter(this, contactViews));
		
		setListAdapter(adapter);
	}
	
	public static String partyName(String code) {
		if (code.equals("D"))
			return "Democrat";
		if (code.equals("R"))
			return "Republican";
		if (code.equals("I"))
			return "Independent";
		else
			return "";
	}
	
	public static String domainName(String domain) {
		if (domain.equals("Upper Seat"))
			return "Senior Senator";
		if (domain.equals("Lower Seat"))
			return "Junior Senator";
		else
			return domain;
	}
	
	public static String pronoun(String gender) {
		if (gender.equals("M"))
			return "his";
		else // "F"
			return "her";
	}
	
	public static String officeName(String office) {
		return office.replaceAll("(?:House|Senate)? ?(?:Office)? ?Building", "").trim();
	}
}