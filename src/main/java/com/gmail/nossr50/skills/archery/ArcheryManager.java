package com.gmail.nossr50.skills.archery;

import com.gmail.nossr50.datatypes.skills.SubSkill;
import com.gmail.nossr50.util.skills.SubSkillActivationType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkill;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.skills.SkillManager;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.skills.SkillUtils;

public class ArcheryManager extends SkillManager {
    public ArcheryManager(McMMOPlayer mcMMOPlayer) {
        super(mcMMOPlayer, PrimarySkill.ARCHERY);
    }

    public boolean canDaze(LivingEntity target) {
        return target instanceof Player && Permissions.isSubSkillEnabled(getPlayer(), SubSkill.ARCHERY_DAZE);
    }

    public boolean canSkillShot() {
        return getSkillLevel() >= Archery.skillShotIncreaseLevel && Permissions.isSubSkillEnabled(getPlayer(), SubSkill.ARCHERY_SKILL_SHOT);
    }

    public boolean canRetrieveArrows() {
        return Permissions.isSubSkillEnabled(getPlayer(), SubSkill.ARCHERY_ARROW_RETRIEVAL);
    }

    /**
     * Calculate bonus XP awarded for Archery when hitting a far-away target.
     *
     * @param target The {@link LivingEntity} damaged by the arrow
     * @param damager The {@link Entity} who shot the arrow
     */
    public void distanceXpBonus(LivingEntity target, Entity damager) {
        Location firedLocation = (Location) damager.getMetadata(mcMMO.arrowDistanceKey).get(0).value();
        Location targetLocation = target.getLocation();

        if (firedLocation.getWorld() != targetLocation.getWorld()) {
            return;
        }

        applyXpGain((int) (Math.min(firedLocation.distanceSquared(targetLocation), 2500) * Archery.DISTANCE_XP_MULTIPLIER), getXPGainReason(target, damager));
    }

    /**
     * Track arrows fired for later retrieval.
     *
     * @param target The {@link LivingEntity} damaged by the arrow
     */
    public void retrieveArrows(LivingEntity target) {
        if (SkillUtils.isActivationSuccessful(SubSkillActivationType.RANDOM_LINEAR_100_SCALE_WITH_CAP, SubSkill.ARCHERY_ARROW_RETRIEVAL, getPlayer(), this.skill, getSkillLevel(), activationChance)) {
            Archery.incrementTrackerValue(target);
        }
    }

    /**
     * Handle the effects of the Daze ability
     *
     * @param defender The {@link Player} being affected by the ability
     */
    public double daze(Player defender) {
        if (!SkillUtils.isActivationSuccessful(SubSkillActivationType.RANDOM_LINEAR_100_SCALE_WITH_CAP, SubSkill.ARCHERY_DAZE, getPlayer(), this.skill, getSkillLevel(), activationChance)) {
            return 0;
        }

        Location dazedLocation = defender.getLocation();
        dazedLocation.setPitch(90 - Misc.getRandom().nextInt(181));

        defender.teleport(dazedLocation);
        defender.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 10, 10));

        if (UserManager.getPlayer(defender).useChatNotifications()) {
            defender.sendMessage(LocaleLoader.getString("Combat.TouchedFuzzy"));
        }

        if (mcMMOPlayer.useChatNotifications()) {
            getPlayer().sendMessage(LocaleLoader.getString("Combat.TargetDazed"));
        }

        return Archery.dazeBonusDamage;
    }

    /**
     * Calculates the damage to deal after Skill Shot has been applied
     *
     * @param oldDamage The raw damage value of this arrow before we modify it
     */
    public double skillShot(double oldDamage) {
        if (!SkillUtils.isActivationSuccessful(SubSkillActivationType.ALWAYS_FIRES, SubSkill.ARCHERY_SKILL_SHOT, getPlayer(), null, 0, 0)) {
            return oldDamage;
        }

        return Archery.getSkillShotBonusDamage(getPlayer(), oldDamage);
    }
}
