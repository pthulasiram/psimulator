/*
 * created 31.1.2012
 */

package networkModule.L3;

import dataStructures.*;
import dataStructures.ipAddresses.IpAddress;
import exceptions.UnsupportedL3TypeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import networkModule.L2.EthernetInterface;
import networkModule.TcpIpNetMod;
import utils.SmartRunnable;
import utils.WorkerThread;

/**
 * Represents IP layer of ISO/OSI model.
 *
 * TODO: pridat paketovy filtr + routovaci tabulku
 * TODO: predelat EthernetInterface na neco abstratniho??
 *
 * @author Stanislav Rehak <rehaksta@fit.cvut.cz>
 */
public class IPLayer implements SmartRunnable {

	protected final WorkerThread worker = new WorkerThread(this);

	/**
	 * ARP cache table.
	 */
	private final ArpCache arpCache = new ArpCache();

	/**
	 * Packet filter.
	 * Controls NAT, packet dropping, ..
	 */
	private final PacketFilter packetFilter = new PacketFilter();

	private final List<ReceiveItem> receiveBuffer = Collections.synchronizedList(new LinkedList<ReceiveItem>());
	private final List<SendItem> sendBuffer = Collections.synchronizedList(new LinkedList<SendItem>());
	/**
	 * Zde budou pakety, ktere je potreba odeslat, ale nemam ARP zaznam, takze byla odeslana ARP request, ale jeste nemam odpoved.
	 * Obsluhovat me bude doMyWork().
	 */
	private final List<SendItem> storeBuffer = Collections.synchronizedList(new LinkedList<SendItem>());
	/**
	 * Routing table with record.
	 */
	private final RoutingTable routingTable = new RoutingTable();
	/**
	 * Link to network module.
	 */
	private final TcpIpNetMod netMod;

	private boolean newArpReply = false;

	private final List<NetworkIface> networkIfaces = new ArrayList<NetworkIface>();

	public IPLayer(TcpIpNetMod netMod) {
		this.netMod = netMod;
		processEthernetInterfaces();
	}

	/**
	 * Potrebne pro vypis pro cisco a linux.
	 * @return
	 */
	public HashMap<IpAddress,ArpCache.ArpRecord> getArpCache() {
		return arpCache.getCache();
	}

	public void receivePacket(L3Packet packet, EthernetInterface iface) {
		receiveBuffer.add(new ReceiveItem(packet, iface));
		worker.wake();
	}

	private void handleReceivePacket(L3Packet packet, EthernetInterface iface) {
		switch (packet.getType()) {
			case ARP:
				ArpPacket arp = (ArpPacket) packet;
				handleReceiveArpPacket(arp, iface);
				break;

			case IPv4:
				IpPacket ip = (IpPacket) packet;
				handleReceiveIpPacket(ip, iface);
				break;

			default:
				throw new UnsupportedL3TypeException("Unsupported L3 type: "+packet.getType());
		}
	}

	private void handleReceiveArpPacket(ArpPacket packet, EthernetInterface iface) {
		// tady se bude resit update cache + reakce
		switch(packet.operation) {
			case ARP_REQUEST:
				// ulozit si odesilatele
				arpCache.updateArpCache(packet.senderIpAddress, packet.senderMacAddress, iface);
				// jsem ja target? Ano -> poslat ARP reply
//				if (packet.targetIpAddress.equals() ) {
					// poslat ARP reply
//				ArpPacket arp = new ArpPacket(packet.senderIpAddress, packet.senderMacAddress, null, iface.getMac()); // TODO: target IP address
//				netMod.ethernetLayer.sendPacket(arp, iface, packet.senderMacAddress);
//				}
				break;

			case ARP_REPLY:
				// ulozit si target
				// kdyz uz to prislo sem, tak je jasne, ze ta odpoved byla pro me, takze si ji muzu ulozit a je to ok
				arpCache.updateArpCache(packet.targetIpAddress, packet.targetMacAddress, iface);
				newArpReply = true; // TODO: domyslet, zda bych nemel reagovat i na ARP_REQUEST, pac se taky muzu dozvedet neco zajimavyho..
				break;
		}
	}

	private void handleArpBuffer() {
		synchronized(storeBuffer) {
			// nebude se moci stat, ze nastavim newArpReply mezitim na true, protoze to bude blokovany..
			// nemelo by zamykat na moc dlouho, protoze zaznamy v ARP cache vydrzi skoro vecnost (4h), takze by buffer mel byt vesmes poloprazdny..

			for (SendItem m : storeBuffer) {
				MacAddress mac = arpCache.getMacAdress(m.dst);
				if (mac != null) {
					// TODO: obslouzit
//					netMod.ethernetLayer.sendPacket(m., null, mac);
				}
			}
			newArpReply = false;
		}
	}

	private void handleReceiveIpPacket(IpPacket packet, EthernetInterface iface) {
		// odnatovat
		// je pro me?
		//		ANO - predat vejs
		//		NE - zaroutovat a predat do ethernetove vrstvy
	}

	public void sendPacket(L4Packet packet, IpAddress dst) {
		sendBuffer.add(new SendItem(packet, dst));
		worker.wake();
	}

	private void handleSendPacket(L4Packet packet, IpAddress dst) {

		// 1) zaroutuj - zjisti odchozi rozhrani
		// 2) zanatuj - packetFilter
		// 3) zjisti MAC adresu z ARP cache - je=OK, neni=vygenerovat ARP request a vlozit do arpBuffer

		// 1
//		routingTable.findRecord(dst);


		// 2
//		packetFilter.

		// 3
//		MacAddress mac = arpCache.getMacAdress(nextHop);
//		if (mac == null) { // posli ARP request a dej do fronty
//			ArpPacket arpPacket = new ArpPacket(dst, mac, dst);
//		}


		throw new UnsupportedOperationException("Not yet implemented");
	}

	public void doMyWork() {

		// prochazet jednotlivy buffery a vyrizovat jednotlivy pakety
		while (!sendBuffer.isEmpty() || !receiveBuffer.isEmpty()) {
			if (!receiveBuffer.isEmpty()) {
				ReceiveItem m = receiveBuffer.remove(0);
				handleReceivePacket(m.packet, m.iface);
			}

			if (!sendBuffer.isEmpty()) {
				SendItem m = sendBuffer.remove(0);
				handleSendPacket(m.packet, m.dst);
			}

			if (newArpReply && !storeBuffer.isEmpty()) {
				// TODO: domyslet, KDY bude obskoceno !!!
				handleArpBuffer();
			}
		}
	}

	/**
	 * Process EthernetInterfaces (L2 iface) and creates adequate NetworkIface (L3 iface).
	 */
	private void processEthernetInterfaces() {
		for (EthernetInterface iface : netMod.ethernetLayer.ifaces) {
			networkIfaces.add(new NetworkIface(iface.name, iface));
		}
	}

	private class SendItem {
		final L4Packet packet;
		final IpAddress dst;

		public SendItem(L4Packet packet, IpAddress dst) {
			this.packet = packet;
			this.dst = dst;
		}
	}

	private class ReceiveItem {
		final L3Packet packet;
		final EthernetInterface iface;

		public ReceiveItem(L3Packet packet, EthernetInterface iface) {
			this.packet = packet;
			this.iface = iface;
		}
	}
}