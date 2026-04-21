package com.fortday.ui;

public final class HudRenderService {
    public String compact(HudSnapshot snapshot) {
        return "HP " + snapshot.health() + " | SH " + snapshot.shield() +
                " | MATS " + snapshot.wood() + "/" + snapshot.brick() + "/" + snapshot.metal() +
                " | AMMO " + snapshot.ammo();
    }
}
