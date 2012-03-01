package com.talkback.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.TextView;

import com.talkback.R;
import com.talkback.model.TalkBackMessage;
import com.talkback.provider.ChatRoom;

/**
 * @author Thanavath Jaroenvanit (thanavath@graphicly.com)
 * 
 */
public class ChatRoomActivity extends Activity {

	private static String LOG_TAG = "TalkBack::ChatRoomActivity";

	private MultiUserChat chatRoom;

	private ArrayList<TalkBackMessage> messages = new ArrayList<TalkBackMessage>();

	private MessagesListAdapter adapter;
	private Handler listHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);
		int chatroom_id = getIntent().getExtras().getInt("_id");
		if (chatroom_id == 0) {
			// should display error message
			this.finish();
		} else {
			Cursor chatroom_cursor = ChatRoom.get(this.getApplicationContext(),
					chatroom_id);
			initPacketListeners();
			initChatView();
			chatroom_cursor.moveToFirst();

			String room_name = chatroom_cursor.getString(chatroom_cursor
					.getColumnIndex(ChatRoom._name));
			String nickname = chatroom_cursor.getString(chatroom_cursor
					.getColumnIndex(ChatRoom._nickname));
			String password = chatroom_cursor.getString(chatroom_cursor
					.getColumnIndex(ChatRoom._password));
			if (password != null)
				chatRoom = ((TalkBackApplication) getApplication()).talkback_user
						.joinChatRoom(getApplicationContext(), room_name,
								nickname, password);
			else
				chatRoom = ((TalkBackApplication) getApplication()).talkback_user
						.joinChatRoom(getApplicationContext(), room_name,
								nickname);

			Log.i(LOG_TAG, "Room " + chatRoom.getRoom());
			Log.i(LOG_TAG, "Nickname " + chatRoom.getNickname());
			Iterator<String> affiliates_it = chatRoom.getOccupants();
			while (affiliates_it.hasNext()) {
				// Occupant aff = affiliates_it.next();
				// Log.i(LOG_TAG, aff.getJid() + " - " + aff.getNick() + " - " +
				// aff.getRole());
				Log.i(LOG_TAG, affiliates_it.next());
			}

			//test get buddies
			Roster roster = ((TalkBackApplication) getApplication()).talkback_user.connection.getRoster();
			Collection<RosterEntry> entries = roster.getEntries();
			for (RosterEntry entry : entries) {
				Log.i(LOG_TAG, entry.getName() + "::" + entry.getUser());
			}
		}

	}

	@Override
	protected void onDestroy() {
		((TalkBackApplication) getApplication()).talkback_user
				.leaveRoom(chatRoom);
		chatRoom = null;
		super.onDestroy();
	}

	private void initPacketListeners() {
		// chat message
		PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
		((TalkBackApplication) getApplication()).talkback_user.connection
				.addPacketListener(new PacketListener() {
					public void processPacket(Packet packet) {
						Message message = (Message) packet;
						Log.d(LOG_TAG, "Packet received");
						Log.d(LOG_TAG, message.toXML());
						if (message.getBody() != null) {
							final TalkBackMessage tb_msg = new TalkBackMessage(
									message);
							listHandler.post(new Runnable() {
								public void run() {
									messages.add(tb_msg);
									adapter.notifyDataSetChanged();
								}
							});
						}
					}
				}, filter);
		// group chat message
		PacketFilter groupchatfilter = new MessageTypeFilter(
				Message.Type.groupchat);
		((TalkBackApplication) getApplication()).talkback_user.connection
				.addPacketListener(new PacketListener() {
					public void processPacket(Packet packet) {
						Message message = (Message) packet;
						Log.d(LOG_TAG, "Packet received");
						Log.d(LOG_TAG, message.toXML());
						if (message.getBody() != null) {
							final TalkBackMessage tb_msg = new TalkBackMessage(
									message);
							listHandler.post(new Runnable() {
								public void run() {
									messages.add(tb_msg);
									adapter.notifyDataSetChanged();
								}
							});
						}
					}
				}, groupchatfilter);

	}

	private void initChatView() {

		ListView messages_view = (ListView) findViewById(R.id.chat_box);
		adapter = new MessagesListAdapter(this.getApplicationContext(),
				messages);
		messages_view.setAdapter(adapter);

		findViewById(R.id.msg_btn).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				TextView text_view = (TextView) findViewById(R.id.msg_input);
				String msg_to_send = text_view.getText().toString();
				if (!msg_to_send.equals("")) {
					try {
						chatRoom.sendMessage(msg_to_send);
					} catch (XMPPException e) {
						e.printStackTrace();
					}
					text_view.setText("");
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}

			}
		});
	}
}
