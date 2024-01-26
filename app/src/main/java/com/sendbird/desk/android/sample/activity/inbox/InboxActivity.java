package com.sendbird.desk.android.sample.activity.inbox;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sendbird.desk.android.Ticket;
import com.sendbird.desk.android.sample.R;
import com.sendbird.desk.android.sample.activity.chat.ChatActivity;
import com.sendbird.desk.android.sample.activity.settings.SettingsActivity;
import com.sendbird.desk.android.sample.app.Event;
import com.sendbird.desk.android.sample.desk.DeskManager;
import com.sendbird.desk.android.sample.utils.PrefUtils;

public class InboxActivity extends AppCompatActivity implements OpenTicketsFragment.OnTicketClosedListener {
    static final int REQUEST_EXIT = 0xf0;

    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            finish();
        }
    });

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

        ViewPager2 viewPager = (ViewPager2) findViewById(R.id.pager);
        viewPager.setAdapter(new PagerAdapter(this));

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText(R.string.desk_open);
                            break;
                        case 1:
                            tab.setText(R.string.desk_closed);
                            break;
                    }
                }
        ).attach();

        FloatingActionButton fabStartChat = (FloatingActionButton) findViewById(R.id.fab_start_chat);
        fabStartChat.setOnClickListener(v -> {
            final String title = "#" + System.currentTimeMillis();
            final String userId = PrefUtils.getUserId();
            startChat(title, userId);
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Event.onEvent(Event.EventListener.INBOX_MOVE_TO_SETTINGS, null);
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            settingsLauncher.launch(intent);
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
    public void onClosed(@NonNull Ticket ticket) {
        if (mClosedTicketsFragment != null) {
            mClosedTicketsFragment.loadClosedTickets(true);
        }
    }

    private OpenTicketsFragment mOpenTicketsFragment;
    private ClosedTicketsFragment mClosedTicketsFragment;

    private class PagerAdapter extends FragmentStateAdapter {
        PagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                if (mOpenTicketsFragment == null) {
                    mOpenTicketsFragment = new OpenTicketsFragment();
                    mOpenTicketsFragment.setOnTicketClosedListener(InboxActivity.this);
                }
                return mOpenTicketsFragment;
            } else {
                if (mClosedTicketsFragment == null) {
                    mClosedTicketsFragment = new ClosedTicketsFragment();
                }
                return mClosedTicketsFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
