/*
 *  Copyright (c) 2020 Private Internet Access, Inc.
 *
 *  This file is part of the Private Internet Access Android Client.
 *
 *  The Private Internet Access Android Client is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  The Private Internet Access Android Client is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with the Private
 *  Internet Access Android Client.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.privateinternetaccess.android.ui.drawer;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.ui.DialogFactory;
import com.privateinternetaccess.android.ui.connection.MainActivity;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

public class ServerListActivity extends BaseActivity {

    public static final int RESULT_SERVER_CHANGED = RESULT_FIRST_USER + 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        bindView();
    }

    private void bindView() {
        Fragment frag = getSupportFragmentManager().findFragmentById(R.id.container);
        if(frag == null){
            frag = new ServerListFragment();
            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
            trans.add(R.id.container, frag);
            trans.commit();
        }
    }

    public void initAppBar() {
        initHeader(true, true);
        setTitle(getString(R.string.drawer_region_selection));
        setGreenBackground();
        setSecondaryGreenBackground();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.right_to_left_exit, R.anim.left_to_right_exit);
        MainActivity.CHANGE_VPN_SERVER_CLOSE = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_region_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        DLog.d("ServerListActivity", "Menu id: " + id);

        if (id == R.id.action_filters) {
            final String[] options = getResources().getStringArray(R.array.region_filters);

            final DialogFactory factory = new DialogFactory(this);
            final Dialog dialog = factory.buildDialog();
            factory.setHeader(getString(R.string.region_dialog_header));

            factory.setPositiveButton(getString(R.string.ok), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Fragment frag = getSupportFragmentManager().findFragmentById(R.id.container);
                    String selectedItem = factory.getSelectedItem();

                    if (frag != null && frag instanceof ServerListFragment) {
                        ServerListFragment serverFrag = (ServerListFragment)frag;

                        if (selectedItem.equals(options[1])) {
                            serverFrag.setUpAdapter(ServerListActivity.this, true, PIAServerHandler.ServerSortingType.PING);
                        }
                        else if (selectedItem.equals(options[2])) {
                            serverFrag.setUpAdapter(ServerListActivity.this, true, PIAServerHandler.ServerSortingType.NAME, PIAServerHandler.ServerSortingType.FAVORITES);
                        }
                        else {
                            serverFrag.setUpAdapter(ServerListActivity.this, true, PIAServerHandler.ServerSortingType.NAME);
                        }

                        Prefs.with(ServerListActivity.this).set(PiaPrefHandler.FILTERS_REGION_SORTING, selectedItem);
                    }

                    dialog.dismiss();
                }
            });

            factory.setNegativeButton(getString(R.string.cancel), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            String selected = Prefs.with(this).get(PiaPrefHandler.FILTERS_REGION_SORTING, options[0]);
            factory.addRadioGroup(options, selected);

            dialog.show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onRadioButtonClicked(View view) {

    }
}