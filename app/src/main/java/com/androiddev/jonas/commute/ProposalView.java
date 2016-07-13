package com.androiddev.jonas.commute;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by jonas on 02.06.15.
 */
public class ProposalView extends RelativeLayout {

    private TextView time;
    private TextView duration;
    private LinearLayout firstMedium; // The first medium
    private LinearLayout secondMedium; // The next medium
    private TextView firstLineNumber;
    private TextView secondLineNumber;
    private ImageView firstLineIcon;
    private ImageView secondLineIcon;
    private ImageView walkingMan;

    public TextView getTime() {
        return time;
    }

    public TextView getDuration() {
        return duration;
    }

    public LinearLayout getFirstMedium() {
        return firstMedium;
    }

    public LinearLayout getSecondMedium() {
        return secondMedium;
    }

    public TextView getFirstLineNumber() {
        return firstLineNumber;
    }

    public TextView getSecondLineNumber() {
        return secondLineNumber;
    }

    public ImageView getFirstLineIcon() {
        return firstLineIcon;
    }

    public ImageView getSecondLineIcon() {
        return secondLineIcon;
    }

    public ImageView getWalkingMan() {
        return walkingMan;
    }

    public ProposalView(Context context) {
        super(context);
        init();
    }

    public ProposalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProposalView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.proposalview, this);
        this.time = (TextView) findViewById(R.id.Proposal_Time);
        this.duration = (TextView) findViewById(R.id.Proposal_Duration);
        this.firstMedium = (LinearLayout) findViewById(R.id.Proposal_FirstMedium);
        this.secondMedium = (LinearLayout) findViewById(R.id.Proposal_SecondMedium);
        this.firstLineNumber = (TextView) findViewById(R.id.Proposal_First_LineNumber);
        this.secondLineNumber = (TextView) findViewById(R.id.Proposal_Second_LineNumber);
        this.firstLineIcon = (ImageView) findViewById(R.id.Proposal_First_LineIcon);
        this.secondLineIcon = (ImageView) findViewById(R.id.Proposal_Second_LineIcon);
        this.walkingMan = (ImageView) findViewById(R.id.Proposal_WalkingMan);
    }
}
