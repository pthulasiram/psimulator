/*
 * UDELAT:
 * TODO: Prepsat metodu prijmiEthernetove()
 */
package pocitac;

import datoveStruktury.*;
import datoveStruktury.CiscoWrapper;

/**
 *
 * @author haldyr
 */
public class CiscoPocitac extends AbstraktniPocitac {

    private CiscoWrapper wrapper;

    public CiscoPocitac(String jmeno, int port) {
        super(jmeno, port);
        wrapper = new CiscoWrapper(this);
    }

    public CiscoWrapper getWrapper() {
        return wrapper;
    }
    /**
     * Vypis metody prijmiEthernetove().
     */
    private boolean debug = false;

    /**
     * Ethernetove prijima nebo odmita prichozi pakety.
     * @param p
     * @param rozhr vstupni rozhrani pocitace, kterej ma paket prijmout, tzn. tohodle pocitace
     * @param ocekavana adresa, kterou odesilaci pocitac na tomto rozhrani ocekava
     * @param sousedni adresa, od ktereho mi prisel ARP request. Linuxu je to jedno, ale
     * pro cisco to je jeden z parametru, podle kteryho se rozhoduje, jestli paket prijme
     * @return true, kdyz byl paket prijmut, jinak false
     *
     * 
     *
     * Ocekavana Ip se vubec nebere v potaz. Prijme paket pouze tehdy, pokud cil paketu je primo na lokalni
     * rozhrani nebo vi kam ho poslat dal dle routovaci tabulky.
     *
     */
    @Override
    public boolean prijmiEthernetove(Paket p, SitoveRozhrani rozhr, IpAdresa ocekavana, IpAdresa sousedni) {

        if (routovaciTabulka.najdiSpravnejZaznam(sousedni) == null) {
            ladici("nemuzu odpovedet na arp dotaz sousedovi, tak smula => neprijimam");
            return false; // kdyz nemuzu odpovedet na arp dotaz sousedovi, tak smula
        }

        if (rozhr.obsahujeStejnouAdresu(ocekavana)) { //adresa souhlasi == je to pro me
            ladici("paket je pro me => prijimam");
            prijmiPaket(p, rozhr);
            return true;
        }

        if (najdiMeziRozhranima(p.cil) != null) { // kdyz vim, kam to poslat dal
            ladici("vim, kam to poslat dal => prijimam");
            prijmiPaket(p, rozhr);
            return true;
        }
        
        // jinak zahazuju
        ladici("muzu odpovedet sousedovi na arp dotaz, neni to pro me a ja nevim kam s tim, tak to radsi neprijmu");
        return false;
    }

    /**
     * Vypisuje pouze kdyz je debug=true.
     * @param s
     */
    public void ladici(String s) {
        if (debug) {
            vypis("Ethernet: " + s);
        }
    }
}
