/*
 * Erstellt am 27.10.2011.
 * TODO: implementovat
 */

package networkModule;

import dataStructures.EthernetPacket;
import dataStructures.L2Packet;
import device.AbstractDevice;
import logging.Loggable;
import logging.Logger;
import logging.LoggingCategory;
import networkModule.L2.EthernetLayer;
import physicalModule.PhysicMod;
import psimulator2.Psimulator;

/**
 * Implementation of network module of generic simple switch.
 * Predpoklada protokol ethernet na vsech rozhranich, ostatni pakety zahazuje.
 * @author neiss
 */
public class SimpleSwitchNetMod extends NetMod  implements Loggable{
	
	public final EthernetLayer ethernetLayer;

    public SimpleSwitchNetMod(EthernetLayer ethernetLayer, AbstractDevice device, PhysicMod physicMod) {
		super(device, physicMod);
		this.ethernetLayer = ethernetLayer;
	}

	/**
	 * Prijimani od fysickyho modulu.
	 * @param packet
	 * @param switchportNumber 
	 */
	@Override
	public void receivePacket(L2Packet packet, int switchportNumber) {
		if (packet.getClass() != EthernetPacket.class) {	//kontrola spravnosti paketu
			Psimulator.getLogger().logg(getDescription(), Logger.WARNING, LoggingCategory.ETHERNET_LAYER,
					"Zahazuju paket, protoze neni ethernetovej, je totiz tridy " + packet.getClass().getName());
		} else {
			ethernetLayer.receivePacket((EthernetPacket)packet, switchportNumber);
		}
	}
	
	

	public String getDescription() {
		return device.getName()+": "+getClass().getName();
	}

	@Override
	public boolean isSwitch() {
		return true;
	}
}