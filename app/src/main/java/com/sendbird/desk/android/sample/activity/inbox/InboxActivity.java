package com.sendbird.desk.android.sample.activity.inbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.sendbird.desk.android.Ticket;
import com.sendbird.desk.android.sample.R;
import com.sendbird.desk.android.sample.activity.chat.ChatActivity;
import com.sendbird.desk.android.sample.activity.settings.SettingsActivity;
import com.sendbird.desk.android.sample.app.Event;
import com.sendbird.desk.android.sample.desk.DeskManager;
import com.sendbird.desk.android.sample.utils.PrefUtils;

public class InboxActivity extends AppCompatActivity implements OpenTicketsFragment.OnTicketClosedListener {

    static final int REQUEST_EXIT = 0xf0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.desk_inbox);
        }

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));

        ((TabLayout) findViewById(R.id.tab_layout)).setupWithViewPager(viewPager);

        FloatingActionButton fabStartChat = (FloatingActionButton) findViewById(R.id.fab_start_chat);
        fabStartChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String title = "#" + System.currentTimeMillis();
                final String userId = PrefUtils.getUserId();
                startChat(title, userId);
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        Event.onEvent(Event.EventListener.INBOX_OPEN_TAB_SELECTED, null);
                        break;
                    case 1:
                        Event.onEvent(Event.EventListener.INBOX_CLOSE_TAB_SELECTED, null);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void startChat(final String title, String userName) {
        Intent intent = new Intent(InboxActivity.this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_TITLE, title);
        intent.putExtra(ChatActivity.EXTRA_USER_NAME, userName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_inbox, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Event.onEvent(Event.EventListener.INBOX_MOVE_TO_SETTINGS, null);
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivityForResult(intent, REQUEST_EXIT);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Event.onEvent(Event.EventListener.INBOX_ENTER, null);
        DeskManager.getInstance().handlePushNotification(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Event.onEvent(Event.EventListener.INBOX_EXIT, null);
        DeskManager.getInstance().handlePushNotification(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EXIT) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
    }

    @Override
    public void onClosed(Ticket ticket) {
        if (mClosedTicketsFragment != null) {
            mClosedTicketsFragment.loadClosedTickets(true);
        }
    }

    private OpenTicketsFragment mOpenTicketsFragment;
    private ClosedTicketsFragment mClosedTicketsFragment;

    private class PagerAdapter extends FragmentPagerAdapter {
        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    if (mOpenTicketsFragment == null) {
                        mOpenTicketsFragment = new OpenTicketsFragment();
                        mOpenTicketsFragment.setOnTicketClosedListener(InboxActivity.this);
                    }
                    return mOpenTicketsFragment;
                case 1:
                    if (mClosedTicketsFragment == null) {
                        mClosedTicketsFragment = new ClosedTicketsFragment();
                    }
                    return mClosedTicketsFragment;
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getResources().getString(R.string.desk_open);
                case 1:
                    return getResources().getString(R.string.desk_closed);
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
