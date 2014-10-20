package de.stefan1200.jts3serverqueryexample;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TeamspeakActionListener;

public class JTS3ServerQueryExample implements TeamspeakActionListener
{
	JTS3ServerQuery query;
	boolean debug = false; // Set to true if you want to see all key / values
	
	public static void main(String[] args)
	{
		JTS3ServerQueryExample jts3 = new JTS3ServerQueryExample();
		
		jts3.runServerMod();
	}
	
	/*
	 * Just output the last error message (if any)
	 */
	void echoError()
	{
		String error = query.getLastError();
		if (error != null)
		{
			System.out.println(error);
			if (query.getLastErrorPermissionID() != -1)
			{
				HashMap<String, String> permInfo = query.getPermissionInfo(query.getLastErrorPermissionID());
				if (permInfo != null)
				{
					System.out.println("Missing Permission: " + permInfo.get("permname"));
				}
			}
		}
	}
	
	/*
	 * Just output all key / value pairs
	 */
	void outputHashMap(HashMap<String, String> hm)
	{
		if (!debug)
		{
			return;
		}
		
		if (hm == null)
		{
			return;
		}
		
	    Collection<String> cValue = hm.values();
	    Collection<String> cKey = hm.keySet();
	    Iterator<String> itrValue = cValue.iterator();
	    Iterator<String> itrKey = cKey.iterator();
		
		while (itrValue.hasNext() && itrKey.hasNext())
		{
			System.out.println(itrKey.next() + ": " + itrValue.next());
		}
	}
	
	public void teamspeakActionPerformed(String eventType, HashMap<String, String> eventInfo)
	{
		System.out.println(eventType + " received");
		outputHashMap(eventInfo);
		
		if (eventType.equals("notifytextmessage"))
		{
			if (eventInfo.get("msg").equalsIgnoreCase("!quitbot")) // Quit this program
			{
				query.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), JTS3ServerQuery.TEXTMESSAGE_TARGET_CLIENT, "Bye Bye, my master!");
				query.removeTeamspeakActionListener();
				query.closeTS3Connection();
				System.exit(0);
			}
			else if (eventInfo.get("msg").equalsIgnoreCase("!clientlist")) // Client List
			{
				Vector<HashMap<String, String>> dataClientList = query.getList(JTS3ServerQuery.LISTMODE_CLIENTLIST, "-info,-times");
				if (dataClientList != null)
				{
					StringBuffer sb = new StringBuffer();
					for (HashMap<String, String> hashMap : dataClientList)
					{
						outputHashMap(hashMap);
						if (sb.length() > 0)
						{
							sb.append(", ");
						}
						sb.append(hashMap.get("client_nickname"));
					}
					query.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), JTS3ServerQuery.TEXTMESSAGE_TARGET_CLIENT, "Client List (only client names displayed): " + sb.toString());
				}
				else
				{
					echoError();
				}
			}
			else if (eventInfo.get("msg").equalsIgnoreCase("!channellist")) // Channel List
			{
				Vector<HashMap<String, String>> dataChannelList = query.getList(JTS3ServerQuery.LISTMODE_CHANNELLIST);
				if (dataChannelList != null)
				{
					StringBuffer sb = new StringBuffer();
					for (HashMap<String, String> hashMap : dataChannelList)
					{
						outputHashMap(hashMap);
						if (sb.length() > 0)
						{
							sb.append(", ");
						}
						sb.append(hashMap.get("channel_name"));
					}
					query.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), JTS3ServerQuery.TEXTMESSAGE_TARGET_CLIENT, "Channel List (only channel names displayed): " + sb.toString());
				}
				else
				{
					echoError();
				}
			}
			else if (eventInfo.get("msg").equalsIgnoreCase("!logview")) // Channel List
			{
				Vector<HashMap<String, String>> dataLogList = query.getLogEntries(4, false, false, 0);
				if (dataLogList != null)
				{
					StringBuffer sb = new StringBuffer();
					for (HashMap<String, String> hashMap : dataLogList)
					{
						outputHashMap(hashMap);
						if (sb.length() > 0)
						{
							sb.append("\n");
						}
						sb.append(hashMap.get("l"));
					}
					query.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), JTS3ServerQuery.TEXTMESSAGE_TARGET_CLIENT, "Log entries (last 4 lines):\n" + sb.toString());
				}
				else
				{
					echoError();
				}
			}
			else if (eventInfo.get("msg").equalsIgnoreCase("!serverinfo")) // Server Info
			{
				HashMap<String, String> dataServerInfo = query.getInfo(JTS3ServerQuery.INFOMODE_SERVERINFO, 0);
				outputHashMap(dataServerInfo);
				query.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), JTS3ServerQuery.TEXTMESSAGE_TARGET_CLIENT, "Server Info (only server name displayed): " + dataServerInfo.get("virtualserver_name"));
			}
		}
	}

	void runServerMod()
	{
		query = new JTS3ServerQuery();
		
		// Connect to TS3 Server, set your server data here
		if (!query.connectTS3Query("localhost", 10011))
		{
			echoError();
			return;
		}
		
		// Login with an server query account. If needed, uncomment next line!
		//query.loginTS3("serveradmin", "password");
		
		// Set our class for receiving events
		query.setTeamspeakActionListener(this);
		
		// Select virtual Server
		if (!query.selectVirtualServer(1))
		{
			echoError();
			return;
		}
		
		// Register some events
		if (!query.addEventNotify(JTS3ServerQuery.EVENT_MODE_TEXTSERVER, 0))  // Server Chat event
		{
			echoError();
			return;
		}
		if (!query.addEventNotify(JTS3ServerQuery.EVENT_MODE_TEXTCHANNEL, 0))  // Channel Chat event
		{
			echoError();
			return;
		}
		if (!query.addEventNotify(JTS3ServerQuery.EVENT_MODE_TEXTPRIVATE, 0))  // Private Chat event
		{
			echoError();
			return;
		}
		
		System.out.println("You can now chat with this program, using server chat,");
		System.out.println("channel chat (in default channel) or by private messaging the query connection!");
		System.out.println("Commands are (some might need serveradmin permissions):");
		System.out.println("!clientlist");
		System.out.println("!channellist");
		System.out.println("!serverinfo");
		System.out.println("!logview");
		System.out.println("!quitbot");
		System.out.println();
		
		while(true)
		{
			try
			{
				/*
				 * Make sure that the Java VM don't quit this program.
				 */
				Thread.sleep(100);
			}
			catch (Exception e)
			{
			}
		}
	}
}
