package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.port.secondary.PeonFirstNameCatalog;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FantasyPeonFirstNameCatalog implements PeonFirstNameCatalog {
    private static final List<String> NAMES = List.of("Aranor", "Beldin", "Celebornel", "Doriak", "Eldamir", "Faelar", "Galdorim", "Haldric", "Isilwen", "Jorund", "Kaelorn", "Luthienor", "Meriadocel", "Nimrodel", "Oronar", "Peregrinor", "Quendar", "Rhovan", "Sylmar", "Thalion", "Urien", "Valandil", "Wendar", "Yavandir", "Zorinel");

    @Override
    public String nameAt(int index) { return NAMES.get(Math.floorMod(index, NAMES.size())); }
    @Override
    public int size() { return NAMES.size(); }
}
