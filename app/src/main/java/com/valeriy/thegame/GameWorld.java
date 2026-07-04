package com.valeriy.thegame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

class GameWorld {
    static final float WORLD_W = 6200f;
    static final float WORLD_H = 6200f;
    private static final float TANK_R = 30f;
    private static final float PLAYER_SPEED = 255f;
    private static final float BOT_SPEED = 205f;
    private static final int TANK_MAX_HP = 180;
    private static final int BULLET_DAMAGE = 30;
    private static final int BASE_BULLET_DAMAGE = 46;
    private static final int MEGA_BASE_BULLET_DAMAGE = 64;
    private static final int BASE_MAX_HP = 1200;
    private static final int ROLE_ATTACK = 1;
    private static final int ROLE_DEFEND = 2;
    private static final int ROLE_PATROL = 3;
    private static final int ROLE_SUPPORT = 4;
    private static final int TEAM_ALLY = 1;
    private static final int TEAM_ENEMY = 2;
    private static final int ALLY_BOT_COUNT = 10;
    private static final int ENEMY_BOT_COUNT = 16;
    private static final int RESERVE_HELP_COUNT = 6;
    private static final int ENEMY_RESERVE_HELP_COUNT = 6;
    private static final int BOT_COUNT = ALLY_BOT_COUNT + ENEMY_BOT_COUNT + RESERVE_HELP_COUNT + ENEMY_RESERVE_HELP_COUNT;
    private static final float BOT_RESPAWN_SECONDS = 6.5f;
    private static final float PLAYER_RESPAWN_SECONDS = 3.0f;
    private static final float WAVE_SECONDS = 34f;
    private static final float WAVE_ATTACK_SECONDS = 24f;
    private static final float HELP_STATION_X = 980f;
    private static final float HELP_STATION_Y = 980f;
    private static final float ENEMY_HELP_STATION_X = WORLD_W - 980f;
    private static final float ENEMY_HELP_STATION_Y = WORLD_H - 980f;
    private static final float[] ASSAULT_LANES = {1120f, 3115f, 4970f};
    private static final float BASE_PRESSURE_DPS_CAP = 28f;
    private static final float MEGA_BASE_PRESSURE_DPS_CAP = 62f;
    private static final float MEGA_BASE_PRESSURE_MULTIPLIER = 1.65f;
    private static final float MEGA_ATTACK_FIRST_DELAY = 58f;
    private static final float MEGA_ATTACK_INTERVAL = 118f;
    private static final float MEGA_ATTACK_DURATION = 44f;
    private static final float MEGA_ATTACK_BANNER_SECONDS = 4.2f;
    private static final int WALL = 1;
    private static final int BARREL = 2;
    private static final int FENCE = 3;
    private static final int BUSH = 4;
    private static final int LAKE = 5;
    private static final int TREE_1 = 6;
    private static final int TREE_2 = 7;
    private static final int AMMO_CRATE = 8;
    private static final int CONCRETE_BARRIER = 9;
    private static final int CRATER = 10;
    private static final int DESTROYED_TANK = 11;
    private static final int FUEL_CANISTERS = 12;
    private static final int SANDBAGS = 13;
    private static final int WATCHTOWER = 14;
    private static final int TALL_GRASS = 15;
    private static final float TERRAIN_TILE = 220f;
    private static final float ROAD_TILE = 190f;
    private static final float TREE_FALL_SECONDS = 0.75f;
    private static final float TREE_REMOVE_SECONDS = 3.0f;
    private static final float MINIMAP_SIZE = 146f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint();
    private final RectF drawRect = new RectF();
    private final RectF tempRect = new RectF();
    private final RectF cameraRect = new RectF();
    private final RectF visibleRect = new RectF();
    private final Bitmap grassTexture;
    private final Bitmap roadTexture;
    private final Bitmap wallTexture;
    private final Bitmap barrelTexture;
    private final Bitmap fenceTexture;
    private final Bitmap bushTexture;
    private final Bitmap lakeTexture;
    private final Bitmap treeOneTexture;
    private final Bitmap treeTwoTexture;
    private final Bitmap tankBlueBodyTexture;
    private final Bitmap tankBlueTurretTexture;
    private final Bitmap tankWhiteBodyTexture;
    private final Bitmap tankWhiteTurretTexture;
    private final Bitmap bulletTexture;
    private final Bitmap muzzleFlashTexture;
    private final Bitmap allyTankBodyTexture;
    private final Bitmap allyTankTurretTexture;
    private final Bitmap blueBaseTexture;
    private final Bitmap enemyBaseTexture;
    private final Bitmap damagedBaseTexture;
    private final Bitmap destroyedBaseTexture;
    private final Bitmap repairZoneTexture;
    private final Bitmap ammoCrateTexture;
    private final Bitmap concreteBarrierTexture;
    private final Bitmap craterTexture;
    private final Bitmap destroyedTankTexture;
    private final Bitmap fuelCanistersTexture;
    private final Bitmap sandbagsTexture;
    private final Bitmap watchtowerTexture;
    private final Bitmap tallGrassTexture;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<RectF> roads = new ArrayList<>();
    private final List<Scar> scars = new ArrayList<>();
    private final List<Crack> cracks = new ArrayList<>();
    private final List<RoadMark> roadMarks = new ArrayList<>();
    private final List<Tank> tanks = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Base> bases = new ArrayList<>();
    private final InputState playerOne = new InputState();
    private final InputState playerTwo = new InputState();
    private final Random random = new Random(8842);

    private boolean authoritative = true;
    private int localPlayerId = 1;
    private float aiClock;
    private boolean playerOneHelpActive;
    private boolean playerTwoHelpActive;
    private float playerOneHelpX;
    private float playerOneHelpY;
    private float playerTwoHelpX;
    private float playerTwoHelpY;
    private boolean enemyHelpActive;
    private float enemyHelpX;
    private float enemyHelpY;
    private float enemyHelpCooldown;
    private float enemyHelpSafeTimer;
    private int megaAttackTeam;
    private float megaAttackTimer;
    private float megaAttackCooldown;
    private float megaBannerTimer;

    GameWorld(Context context) {
        paint.setFilterBitmap(false);
        bitmapPaint.setFilterBitmap(false);
        bitmapPaint.setDither(false);
        bitmapPaint.setAlpha(255);
        grassTexture = loadTexture(context, "textures/grass.png");
        roadTexture = loadTexture(context, "textures/road.png");
        wallTexture = loadTexture(context, "textures/wall_block.png");
        barrelTexture = loadTexture(context, "textures/barrel.png");
        fenceTexture = loadTexture(context, "textures/fence.png");
        bushTexture = loadTexture(context, "textures/bush.png");
        lakeTexture = loadTexture(context, "textures/lake.png");
        treeOneTexture = loadTexture(context, "textures/tree_type_1.png");
        treeTwoTexture = loadTexture(context, "textures/tree_type_2.png");
        tankBlueBodyTexture = loadTexture(context, "textures/tank_blue_body.png");
        tankBlueTurretTexture = loadTexture(context, "textures/tank_blue_turret.png");
        tankWhiteBodyTexture = loadTexture(context, "textures/tank_white_body.png");
        tankWhiteTurretTexture = loadTexture(context, "textures/tank_white_turret.png");
        bulletTexture = loadTexture(context, "textures/bullet.png");
        muzzleFlashTexture = loadTexture(context, "textures/muzzle_flash.png");
        allyTankBodyTexture = loadTexture(context, "textures/tank_ally_body.png");
        allyTankTurretTexture = loadTexture(context, "textures/tank_ally_turret.png");
        blueBaseTexture = loadTexture(context, "textures/base_blue.png");
        enemyBaseTexture = loadTexture(context, "textures/base_enemy.png");
        damagedBaseTexture = loadTexture(context, "textures/base_damaged.png");
        destroyedBaseTexture = loadTexture(context, "textures/base_destroyed.png");
        repairZoneTexture = loadTexture(context, "textures/repair_zone.png");
        ammoCrateTexture = loadTexture(context, "textures/ammo_crate.png");
        concreteBarrierTexture = loadTexture(context, "textures/concrete_barrier.png");
        craterTexture = loadTexture(context, "textures/crater.png");
        destroyedTankTexture = loadTexture(context, "textures/destroyed_tank.png");
        fuelCanistersTexture = loadTexture(context, "textures/fuel_canisters.png");
        sandbagsTexture = loadTexture(context, "textures/sandbags.png");
        watchtowerTexture = loadTexture(context, "textures/watchtower.png");
        tallGrassTexture = loadTexture(context, "textures/tall_grass.png");
        buildMap();
        reset(true, 1);
    }

    synchronized void reset(boolean authoritative, int localPlayerId) {
        reset(authoritative, localPlayerId, false);
    }

    synchronized void reset(boolean authoritative, int localPlayerId, boolean multiplayer) {
        this.authoritative = authoritative;
        this.localPlayerId = localPlayerId;
        aiClock = 0f;
        playerOneHelpActive = false;
        playerTwoHelpActive = false;
        enemyHelpActive = false;
        enemyHelpCooldown = 0f;
        enemyHelpSafeTimer = 0f;
        megaAttackTeam = 0;
        megaAttackTimer = 0f;
        megaAttackCooldown = MEGA_ATTACK_FIRST_DELAY;
        megaBannerTimer = 0f;
        tanks.clear();
        bullets.clear();
        bases.clear();
        bases.add(new Base(TEAM_ALLY, 350f, 770f, 300f, blueBaseTexture));
        bases.add(new Base(TEAM_ENEMY, WORLD_W - 520f, WORLD_H - 520f, 330f, enemyBaseTexture));

        tanks.add(new Tank(1, 380f, 380f, false, TEAM_ALLY, 0xff69d2e7, "P1"));
        if (multiplayer) {
            tanks.add(new Tank(2, 560f, 430f, false, TEAM_ALLY, 0xff69d2e7, "P2"));
        }
        for (int i = 0; i < ALLY_BOT_COUNT; i++) {
            float x = 700f + (i % 3) * 150f + random.nextInt(70);
            float y = 690f + (i / 3) * 150f + random.nextInt(70);
            Tank ally = new Tank(100 + i, x, y, true, TEAM_ALLY, 0xff2dd4bf, "A" + (i + 1));
            ally.role = i < 2 ? ROLE_DEFEND : (i < 5 ? ROLE_ATTACK : (i < 7 ? ROLE_PATROL : ROLE_SUPPORT));
            ally.guard = ally.role == ROLE_DEFEND;
            tanks.add(ally);
        }
        for (int i = 0; i < ENEMY_BOT_COUNT; i++) {
            float x = WORLD_W - 1120f + (i % 4) * 170f + random.nextInt(80);
            float y = WORLD_H - 1120f + (i / 4) * 170f + random.nextInt(80);
            Tank enemy = new Tank(200 + i, x, y, true, TEAM_ENEMY, 0xffef476f, "E" + (i + 1));
            enemy.role = i < 4 ? ROLE_DEFEND : (i < 10 ? ROLE_ATTACK : (i < 12 ? ROLE_PATROL : ROLE_SUPPORT));
            enemy.guard = enemy.role == ROLE_DEFEND;
            tanks.add(enemy);
        }
        for (int i = 0; i < RESERVE_HELP_COUNT; i++) {
            float x = HELP_STATION_X + (i % 3 - 1f) * 98f;
            float y = HELP_STATION_Y + 45f + (i / 3) * 105f;
            Tank helper = new Tank(150 + i, x, y, true, TEAM_ALLY, 0xff22c55e, "H" + (i + 1));
            helper.role = ROLE_SUPPORT;
            helper.reserveHelper = true;
            helper.stationX = x;
            helper.stationY = y;
            helper.bodyAngle = -(float) Math.PI * 0.5f;
            helper.turretAngle = helper.bodyAngle;
            tanks.add(helper);
        }
        for (int i = 0; i < ENEMY_RESERVE_HELP_COUNT; i++) {
            float x = ENEMY_HELP_STATION_X + (i % 3 - 1f) * 98f;
            float y = ENEMY_HELP_STATION_Y - 45f - (i / 3) * 105f;
            Tank helper = new Tank(250 + i, x, y, true, TEAM_ENEMY, 0xffdc2626, "R" + (i + 1));
            helper.role = ROLE_SUPPORT;
            helper.reserveHelper = true;
            helper.stationX = x;
            helper.stationY = y;
            helper.bodyAngle = (float) Math.PI * 0.5f;
            helper.turretAngle = helper.bodyAngle;
            tanks.add(helper);
        }
        playerOne.set(0, 0, 1, 0, false);
        playerTwo.set(0, 0, -1, 0, false);
    }

    synchronized void setInput(int playerId, InputState input) {
        if (playerId == 1) {
            playerOne.set(input.moveX, input.moveY, input.aimX, input.aimY, input.shooting);
            playerOne.setHelpRequested(input.helpRequested);
            playerOne.setStopHelpRequested(input.stopHelpRequested);
        } else if (playerId == 2) {
            playerTwo.set(input.moveX, input.moveY, input.aimX, input.aimY, input.shooting);
            playerTwo.setHelpRequested(input.helpRequested);
            playerTwo.setStopHelpRequested(input.stopHelpRequested);
        }
    }

    synchronized void update(float dt) {
        if (!authoritative) return;
        aiClock += dt;
        updateMegaAttack(dt);
        handleHelpRequests();
        updateEnemyHelp(dt);
        updateRespawns(dt);
        updateReserveHelpers(dt);
        updateTank(getTank(1), playerOne.copy(), dt, PLAYER_SPEED);
        updateTank(getTank(2), playerTwo.copy(), dt, PLAYER_SPEED);
        for (Tank tank : tanks) {
            if (tank.bot) updateTank(tank, botBrain(tank, aiClock), dt, BOT_SPEED);
        }
        updateRepairZones(dt);
        updateObstacles(dt);
        updateBullets(dt);
        updateBasePressure(dt);
    }

    synchronized int getLocalPlayerId() {
        return localPlayerId;
    }

    synchronized float getLocalX() {
        Tank tank = getTank(localPlayerId);
        return tank == null ? WORLD_W * 0.5f : tank.x;
    }

    synchronized float getLocalY() {
        Tank tank = getTank(localPlayerId);
        return tank == null ? WORLD_H * 0.5f : tank.y;
    }

    synchronized void draw(Canvas canvas, int width, int height) {
        Tank local = getTank(localPlayerId);
        float cx = local == null ? WORLD_W * 0.5f : local.x;
        float cy = local == null ? WORLD_H * 0.5f : local.y;
        float camX = clamp(cx - width * 0.5f, 0f, Math.max(0f, WORLD_W - width));
        float camY = clamp(cy - height * 0.5f, 0f, Math.max(0f, WORLD_H - height));
        cameraRect.set(camX, camY, camX + width, camY + height);
        visibleRect.set(camX - 96f, camY - 96f, camX + width + 96f, camY + height + 96f);

        canvas.drawColor(0xff16202a);
        canvas.save();
        canvas.translate(-camX, -camY);
        drawTerrain(canvas, camX, camY, width, height);
        drawRoads(canvas, camX, camY, width, height);
        drawScars(canvas, camX, camY, width, height);
        drawCracks(canvas, camX, camY, width, height);
        drawRoadMarks(canvas, camX, camY, width, height);
        drawHelpStation(canvas, HELP_STATION_X, HELP_STATION_Y, "HELP STATION", playerOneHelpActive || playerTwoHelpActive, true);
        drawHelpStation(canvas, ENEMY_HELP_STATION_X, ENEMY_HELP_STATION_Y, "ENEMY RESERVE", enemyHelpActive, false);
        drawWorldBounds(canvas);
        for (Base base : bases) {
            if (RectF.intersects(visibleRect, base.rect)) base.drawRepair(canvas, this);
        }
        for (Base base : bases) {
            if (RectF.intersects(visibleRect, base.rect)) base.draw(canvas, paint, this);
        }
        for (Obstacle obstacle : obstacles) {
            if (!obstacle.gone && !obstacle.foreground() && RectF.intersects(visibleRect, obstacle.rect)) {
                obstacle.draw(canvas, paint, this);
            }
        }
        drawAimGuide(canvas, paint, local);
        for (Bullet bullet : bullets) {
            if (visibleRect.contains(bullet.x, bullet.y)) bullet.draw(canvas, paint, this);
        }
        for (Tank tank : tanks) {
            if (tank.hp > 0 && visibleRect.contains(tank.x, tank.y)) {
                tank.draw(canvas, paint, this, tank.id == localPlayerId);
            }
        }
        for (Obstacle obstacle : obstacles) {
            if (!obstacle.gone && obstacle.foreground() && RectF.intersects(visibleRect, obstacle.rect)) {
                obstacle.draw(canvas, paint, this);
            }
        }
        canvas.restore();
        drawHud(canvas, width, height, cx, cy);
    }

    private void drawAimGuide(Canvas canvas, Paint paint, Tank tank) {
        if (tank == null || tank.hp <= 0) return;
        float startX = tank.x + (float) Math.cos(tank.turretAngle) * 52f;
        float startY = tank.y + (float) Math.sin(tank.turretAngle) * 52f;
        float endX = tank.x + (float) Math.cos(tank.turretAngle) * 420f;
        float endY = tank.y + (float) Math.sin(tank.turretAngle) * 420f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(0xaae8f7ff);
        canvas.drawLine(startX, startY, endX, endY, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xdde8f7ff);
        canvas.drawCircle(endX, endY, 8f, paint);
        paint.setColor(0x88000000);
        canvas.drawCircle(endX, endY, 3f, paint);
    }

    synchronized String snapshot() {
        StringBuilder out = new StringBuilder(2048);
        out.append("SNAP|");
        for (Tank t : tanks) {
            out.append(String.format(Locale.US, "T,%d,%.1f,%.1f,%.3f,%.3f,%d,%d,%d;",
                    t.id, t.x, t.y, t.bodyAngle, t.turretAngle, t.hp, t.bot ? 1 : 0, t.team));
        }
        out.append("|");
        for (Bullet b : bullets) {
            out.append(String.format(Locale.US, "B,%.1f,%.1f,%.1f,%.1f,%d,%d;", b.x, b.y, b.vx, b.vy, b.owner, b.team));
        }
        out.append("|");
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle o = obstacles.get(i);
            if (o.isTree() && (o.falling || o.gone)) {
                out.append(String.format(Locale.US, "O,%d,%.2f,%.1f,%d;", i, o.fallTime, o.fallSign, o.gone ? 1 : 0));
            }
        }
        out.append("|");
        for (Base base : bases) {
            out.append(String.format(Locale.US, "BA,%d,%d;", base.team, base.hp));
        }
        out.append("|");
        out.append(String.format(Locale.US, "M,%d,%.2f,%.2f;", megaAttackTeam, megaAttackTimer, megaBannerTimer));
        return out.toString();
    }

    synchronized void applySnapshot(String snapshot) {
        if (snapshot == null || !snapshot.startsWith("SNAP|")) return;
        String[] parts = snapshot.split("\\|", -1);
        if (parts.length < 3) return;
        tanks.clear();
        bullets.clear();
        for (String item : parts[1].split(";")) {
            if (item.length() < 2) continue;
            String[] p = item.split(",");
            if (p.length < 8 || !"T".equals(p[0])) continue;
            try {
                int id = Integer.parseInt(p[1]);
                boolean bot = "1".equals(p[7]);
                int team = p.length > 8 ? Integer.parseInt(p[8]) : (bot ? TEAM_ENEMY : TEAM_ALLY);
                Tank t = new Tank(Integer.parseInt(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3]),
                        bot, team, team == TEAM_ALLY ? 0xff2dd4bf : 0xffef476f,
                        bot ? (team == TEAM_ALLY ? "A" : "E") : "P" + id);
                t.bodyAngle = Float.parseFloat(p[4]);
                t.turretAngle = Float.parseFloat(p[5]);
                t.hp = Integer.parseInt(p[6]);
                tanks.add(t);
            } catch (NumberFormatException ignored) {
            }
        }
        for (String item : parts[2].split(";")) {
            if (item.length() < 2) continue;
            String[] p = item.split(",");
            if (p.length < 6 || !"B".equals(p[0])) continue;
            try {
                float vx = Float.parseFloat(p[3]);
                float vy = Float.parseFloat(p[4]);
                int owner = Integer.parseInt(p[5]);
                int team = p.length > 6 ? Integer.parseInt(p[6]) : ownerTeam(owner);
                bullets.add(new Bullet(Float.parseFloat(p[1]), Float.parseFloat(p[2]), vx, vy, owner, team, (float) Math.atan2(vy, vx)));
            } catch (NumberFormatException ignored) {
            }
        }
        if (parts.length > 3) applyObstacleSnapshot(parts[3]);
        if (parts.length > 4) applyBaseSnapshot(parts[4]);
        if (parts.length > 5) applyMegaSnapshot(parts[5]);
    }

    private void updateTank(Tank tank, InputState input, float dt, float speed) {
        if (tank == null || tank.hp <= 0) return;
        float mx = input.moveX;
        float my = input.moveY;
        float len = (float) Math.hypot(mx, my);
        float oldX = tank.x;
        float oldY = tank.y;
        if (len > 0.08f) {
            mx /= len;
            my /= len;
            tank.bodyAngle = (float) Math.atan2(my, mx);
            float nextX = clamp(tank.x + mx * speed * dt, TANK_R, WORLD_W - TANK_R);
            float nextY = clamp(tank.y + my * speed * dt, TANK_R, WORLD_H - TANK_R);
            if (!blockedForTank(nextX, nextY, TANK_R)) {
                tank.x = nextX;
                tank.y = nextY;
            } else if (!blockedForTank(nextX, oldY, TANK_R)) {
                tank.x = nextX;
                tank.y = oldY;
            } else if (!blockedForTank(oldX, nextY, TANK_R)) {
                tank.x = oldX;
                tank.y = nextY;
            } else {
                tank.x = oldX;
                tank.y = oldY;
            }
            toppleTrees(tank.x, tank.y, TANK_R, tank.bodyAngle);
        }
        tank.turretAngle = (float) Math.atan2(input.aimY, input.aimX);
        tank.reload = Math.max(0f, tank.reload - dt);
        tank.flash = Math.max(0f, tank.flash - dt);
        if (input.shooting && tank.reload <= 0f) {
            float bx = tank.x + (float) Math.cos(tank.turretAngle) * 38f;
            float by = tank.y + (float) Math.sin(tank.turretAngle) * 38f;
            bullets.add(new Bullet(bx, by, (float) Math.cos(tank.turretAngle) * 760f, (float) Math.sin(tank.turretAngle) * 760f, tank.id, tank.team, tank.turretAngle));
            tank.flash = 0.09f;
            tank.reload = tank.bot ? 0.9f : 0.42f;
        }
    }

    private InputState botBrain(Tank bot, float time) {
        if (bot.hp <= 0) return new InputState();
        if (bot.reserveHelper) return reserveBrain(bot, time);
        Base homeBase = getBase(bot.team);
        Base enemyBase = getBase(bot.team == TEAM_ALLY ? TEAM_ENEMY : TEAM_ALLY);
        Tank closeThreat = nearestEnemy(bot, bot.role == ROLE_ATTACK ? 860f : 1040f);
        Tank baseIntruder = nearestEnemyNearBase(bot, homeBase, 1180f);
        boolean megaAttack = megaAttackActiveFor(bot.team);
        boolean megaDefense = megaAttackActiveAgainst(bot.team);

        if (bot.hp < 52 && homeBase != null && !homeBase.destroyed()) {
            return retreatToRepair(bot, homeBase, closeThreat, time);
        }

        if (megaDefense && homeBase != null && !homeBase.destroyed()
                && (bot.role == ROLE_DEFEND || bot.role == ROLE_PATROL || bot.id % 2 == 0)) {
            if (baseIntruder != null) return attackTank(bot, baseIntruder, time, 1040f);
            Tank threat = nearestEnemyNearPoint(homeBase.x, homeBase.y, bot.team, 1600f);
            if (threat != null) return attackTank(bot, threat, time, 990f);
            return guardBase(bot, homeBase, time);
        }

        boolean baseAlarm = homeBase != null && !homeBase.destroyed()
                && (homeBase.hp < BASE_MAX_HP * 0.72f || baseIntruder != null);
        if (baseAlarm && (bot.role == ROLE_DEFEND || bot.role == ROLE_PATROL || bot.id % 3 == 0)) {
            if (baseIntruder != null) return attackTank(bot, baseIntruder, time, 980f);
            return guardBase(bot, homeBase, time);
        }

        if (closeThreat != null && shouldFight(bot, closeThreat, enemyBase)) {
            if (bot.role != ROLE_ATTACK || distance(bot.x, bot.y, closeThreat.x, closeThreat.y) < 470f) {
                return attackTank(bot, closeThreat, time, bot.role == ROLE_DEFEND ? 980f : 900f);
            }
        }

        if (bot.role == ROLE_DEFEND) {
            if (baseIntruder != null) return attackTank(bot, baseIntruder, time, 980f);
            return guardBase(bot, homeBase, time);
        }

        if (megaAttack && enemyBase != null && !enemyBase.destroyed()) {
            return attackObjective(bot, enemyBase, time, true);
        }

        if (bot.role == ROLE_SUPPORT) {
            if (waveAttacking(bot.team, time) && enemyBase != null && !enemyBase.destroyed()) {
                return attackObjective(bot, enemyBase, time);
            }
            return supportFront(bot, time);
        }

        if (bot.role == ROLE_PATROL && !waveAttacking(bot.team, time)) {
            return patrolFront(bot, time);
        }

        if (enemyBase != null && !enemyBase.destroyed()) {
            if (bot.role == ROLE_ATTACK) {
                return attackObjective(bot, enemyBase, time);
            }
            if (!waveAttacking(bot.team, time) && distanceToBase(bot, enemyBase) > 1700f) {
                return moveToRally(bot, time);
            }
            return attackObjective(bot, enemyBase, time);
        }
        return patrolFront(bot, time);
    }

    private InputState attackTank(Tank bot, Tank target, float time, float fireRange) {
        InputState input = new InputState();
        float dx = target.x - bot.x;
        float dy = target.y - bot.y;
        float dist = (float) Math.hypot(dx, dy);
        float aimAngle = (float) Math.atan2(dy, dx);
        float distanceMiss = clamp(dist / 1200f, 0f, 1f) * 0.26f;
        float wobble = (float) Math.sin(time * 2.1f + bot.id * 0.73f) * 0.12f;
        float finalAim = aimAngle + wobble + bot.aimNoise * distanceMiss;
        float aimX = (float) Math.cos(finalAim);
        float aimY = (float) Math.sin(finalAim);
        float strafe = (float) Math.sin(time * 1.4f + bot.id) * 0.45f;
        float moveX = dist > 360f ? aimX - aimY * strafe : -aimX * 0.35f - aimY * strafe;
        float moveY = dist > 360f ? aimY + aimX * strafe : -aimY * 0.35f + aimX * strafe;
        float[] move = avoidMove(bot, moveX, moveY, 150f);
        moveX = move[0];
        moveY = move[1];
        input.set(moveX, moveY, aimX, aimY, dist < fireRange && clearShot(bot.x, bot.y, target.x, target.y));
        return input;
    }

    private InputState attackBase(Tank bot, Base targetBase, float time) {
        InputState input = new InputState();
        float dx = targetBase.x - bot.x;
        float dy = targetBase.y - bot.y;
        float dist = (float) Math.hypot(dx, dy);
        float aimAngle = (float) Math.atan2(dy, dx);
        float wobble = (float) Math.sin(time * 1.7f + bot.id) * 0.08f;
        float aimX = (float) Math.cos(aimAngle + wobble);
        float aimY = (float) Math.sin(aimAngle + wobble);
        float moveX = dist > 900f ? dx / Math.max(1f, dist) : -dx / Math.max(1f, dist) * 0.12f;
        float moveY = dist > 900f ? dy / Math.max(1f, dist) : -dy / Math.max(1f, dist) * 0.12f;
        float[] move = avoidMove(bot, moveX, moveY, 150f);
        moveX = move[0];
        moveY = move[1];
        input.set(moveX, moveY, aimX, aimY, dist < 1160f && clearShot(bot.x, bot.y, targetBase.x, targetBase.y));
        return input;
    }

    private InputState attackObjective(Tank bot, Base targetBase, float time) {
        return attackObjective(bot, targetBase, time, false);
    }

    private InputState attackObjective(Tank bot, Base targetBase, float time, boolean urgent) {
        Tank blocker = nearestEnemy(bot, urgent ? 560f : (bot.team == TEAM_ALLY ? 760f : 820f));
        if (blocker != null && shouldFightOnRoute(bot, blocker, targetBase, urgent)) {
            return attackTank(bot, blocker, time, 920f);
        }
        if (distanceToBase(bot, targetBase) < (urgent ? 1540f : 1320f)) {
            return attackBase(bot, targetBase, time);
        }

        float laneY = assaultLane(bot);
        float targetX;
        float targetY;
        if (bot.team == TEAM_ALLY) {
            if (bot.x < 1880f) {
                targetX = 1980f;
                targetY = laneY + laneOffset(bot, time, 120f);
            } else if (bot.x < 3300f) {
                targetX = 3300f + laneOffset(bot, time, 90f);
                targetY = laneY;
            } else if (bot.x < WORLD_W - 1820f) {
                targetX = WORLD_W - 1540f;
                targetY = laneY + laneOffset(bot, time, 70f);
            } else {
                targetX = targetBase.x - 760f;
                targetY = targetBase.y - 420f + laneOffset(bot, time, 180f);
            }
        } else {
            if (bot.x > WORLD_W - 1880f) {
                targetX = WORLD_W - 1980f;
                targetY = laneY + laneOffset(bot, time, 120f);
            } else if (bot.x > 3000f) {
                targetX = 3000f + laneOffset(bot, time, 90f);
                targetY = laneY;
            } else if (bot.x > 1820f) {
                targetX = 1540f;
                targetY = laneY + laneOffset(bot, time, 70f);
            } else {
                targetX = targetBase.x + 760f;
                targetY = targetBase.y + 360f + laneOffset(bot, time, 160f);
            }
        }

        float aim = (float) Math.atan2(targetBase.y - bot.y, targetBase.x - bot.x);
        return moveToward(bot, targetX, targetY, aim, false);
    }

    private boolean shouldFightOnRoute(Tank bot, Tank target, Base targetBase) {
        return shouldFightOnRoute(bot, target, targetBase, false);
    }

    private boolean shouldFightOnRoute(Tank bot, Tank target, Base targetBase, boolean urgent) {
        float dist = distance(bot.x, bot.y, target.x, target.y);
        if (dist < (urgent ? 360f : 430f)) return true;
        if (clearShot(bot.x, bot.y, target.x, target.y) && dist < (urgent ? 500f : 720f)) return true;
        return !urgent && distance(target.x, target.y, targetBase.x, targetBase.y) < 980f;
    }

    private float assaultLane(Tank bot) {
        int index = Math.abs(bot.id) % ASSAULT_LANES.length;
        if (bot.team == TEAM_ENEMY && index == 0) index = 1;
        return ASSAULT_LANES[index];
    }

    private float laneOffset(Tank bot, float time, float amount) {
        return (float) Math.sin(time * 0.42f + bot.id * 0.73f) * amount;
    }

    private InputState guardBase(Tank bot, Base base, float time) {
        InputState input = new InputState();
        if (base == null) return input;
        float angle = time * 0.45f + bot.id * 1.37f;
        float px = base.x + (float) Math.cos(angle) * 360f;
        float py = base.y + (float) Math.sin(angle) * 280f;
        float dx = px - bot.x;
        float dy = py - bot.y;
        float dist = (float) Math.hypot(dx, dy);
        float moveX = dist > 80f ? dx / Math.max(1f, dist) : 0f;
        float moveY = dist > 80f ? dy / Math.max(1f, dist) : 0f;
        input.set(moveX, moveY, (float) Math.cos(angle), (float) Math.sin(angle), false);
        return input;
    }

    private InputState reserveBrain(Tank bot, float time) {
        if (bot.helpActive) {
            Tank target = nearestEnemyNearPoint(bot.helpX, bot.helpY, bot.team, 1500f);
            if (target == null) target = nearestEnemy(bot, 1150f);
            if (target != null) return braveAttackTank(bot, target, time, 1080f);
            Base enemyBase = getBase(bot.team == TEAM_ALLY ? TEAM_ENEMY : TEAM_ALLY);
            if (enemyBase != null && !enemyBase.destroyed() && distance(bot.x, bot.y, enemyBase.x, enemyBase.y) < 1320f) {
                return braveAttackBase(bot, enemyBase, time);
            }
            if (distance(bot.x, bot.y, bot.helpX, bot.helpY) > 120f) {
                float aim = (float) Math.atan2(bot.helpY - bot.y, bot.helpX - bot.x);
                return reserveMoveToward(bot, bot.helpX, bot.helpY, aim, false);
            }
            float watch = time * 0.8f + bot.id;
            InputState input = new InputState();
            input.set(0f, 0f, (float) Math.cos(watch), (float) Math.sin(watch), false);
            return input;
        }
        if (distance(bot.x, bot.y, bot.stationX, bot.stationY) > 70f) {
            return reserveMoveToward(bot, bot.stationX, bot.stationY, -(float) Math.PI * 0.5f, false);
        }
        InputState idle = new InputState();
        idle.set(0f, 0f, 0f, -1f, false);
        return idle;
    }

    private InputState reserveMoveToward(Tank bot, float targetX, float targetY, float aimAngle, boolean shooting) {
        updateReserveDetour(bot, targetX, targetY);
        if (bot.detourUntil > aiClock) {
            if (distance(bot.x, bot.y, bot.detourX, bot.detourY) < 95f) {
                bot.detourUntil = 0f;
            } else {
                return moveToward(bot, bot.detourX, bot.detourY, aimAngle, shooting);
            }
        }
        return moveToward(bot, targetX, targetY, aimAngle, shooting);
    }

    private void updateReserveDetour(Tank bot, float targetX, float targetY) {
        if (distance(bot.x, bot.y, targetX, targetY) < 180f) {
            bot.stuckTime = 0f;
            return;
        }
        if (bot.lastNavX == 0f && bot.lastNavY == 0f) {
            bot.lastNavX = bot.x;
            bot.lastNavY = bot.y;
            return;
        }
        float moved = distance(bot.x, bot.y, bot.lastNavX, bot.lastNavY);
        if (moved < 2.2f) {
            bot.stuckTime += 0.033f;
        } else {
            bot.stuckTime = Math.max(0f, bot.stuckTime - 0.08f);
            bot.lastNavX = bot.x;
            bot.lastNavY = bot.y;
        }
        if (bot.stuckTime < 0.72f || bot.detourUntil > aiClock) return;

        float dx = targetX - bot.x;
        float dy = targetY - bot.y;
        float len = Math.max(1f, (float) Math.hypot(dx, dy));
        dx /= len;
        dy /= len;
        float side = bot.avoidSide;
        float px = -dy * side;
        float py = dx * side;
        float detourX = clamp(bot.x + px * 520f + dx * 160f, TANK_R, WORLD_W - TANK_R);
        float detourY = clamp(bot.y + py * 520f + dy * 160f, TANK_R, WORLD_H - TANK_R);
        if (blockedForTank(detourX, detourY, TANK_R + 12f)) {
            bot.avoidSide = -bot.avoidSide;
            px = -dy * bot.avoidSide;
            py = dx * bot.avoidSide;
            detourX = clamp(bot.x + px * 520f + dx * 160f, TANK_R, WORLD_W - TANK_R);
            detourY = clamp(bot.y + py * 520f + dy * 160f, TANK_R, WORLD_H - TANK_R);
        }
        bot.detourX = detourX;
        bot.detourY = detourY;
        bot.detourUntil = aiClock + 3.2f;
        bot.stuckTime = 0f;
        bot.lastNavX = bot.x;
        bot.lastNavY = bot.y;
    }

    private InputState braveAttackTank(Tank bot, Tank target, float time, float fireRange) {
        InputState input = new InputState();
        float dx = target.x - bot.x;
        float dy = target.y - bot.y;
        float dist = Math.max(1f, (float) Math.hypot(dx, dy));
        float aimAngle = (float) Math.atan2(dy, dx);
        float wobble = (float) Math.sin(time * 2.4f + bot.id * 0.61f) * 0.07f;
        float finalAim = aimAngle + wobble + bot.aimNoise * 0.12f;
        float aimX = (float) Math.cos(finalAim);
        float aimY = (float) Math.sin(finalAim);
        float strafe = (float) Math.sin(time * 1.8f + bot.id) * 0.34f;
        float forward = dist > 250f ? 1f : 0.24f;
        float moveX = dx / dist * forward - aimY * strafe;
        float moveY = dy / dist * forward + aimX * strafe;
        float[] move = avoidMove(bot, moveX, moveY, 220f);
        moveX = move[0];
        moveY = move[1];
        input.set(moveX, moveY, aimX, aimY, dist < fireRange && clearShot(bot.x, bot.y, target.x, target.y));
        return input;
    }

    private InputState braveAttackBase(Tank bot, Base targetBase, float time) {
        InputState input = new InputState();
        float dx = targetBase.x - bot.x;
        float dy = targetBase.y - bot.y;
        float dist = Math.max(1f, (float) Math.hypot(dx, dy));
        float aimAngle = (float) Math.atan2(dy, dx);
        float wobble = (float) Math.sin(time * 1.9f + bot.id) * 0.05f;
        float aimX = (float) Math.cos(aimAngle + wobble);
        float aimY = (float) Math.sin(aimAngle + wobble);
        float moveX = dist > 520f ? dx / dist : 0f;
        float moveY = dist > 520f ? dy / dist : 0f;
        float[] move = avoidMove(bot, moveX, moveY, 220f);
        moveX = move[0];
        moveY = move[1];
        input.set(moveX, moveY, aimX, aimY, dist < 1100f && clearShot(bot.x, bot.y, targetBase.x, targetBase.y));
        return input;
    }

    private void handleHelpRequests() {
        if (playerOne.stopHelpRequested) {
            stopHelpFor(1);
            playerOne.setStopHelpRequested(false);
        }
        if (playerTwo.stopHelpRequested) {
            stopHelpFor(2);
            playerTwo.setStopHelpRequested(false);
        }
        if (playerOne.helpRequested) {
            callHelpFor(1);
            playerOne.setHelpRequested(false);
        }
        if (playerTwo.helpRequested) {
            callHelpFor(2);
            playerTwo.setHelpRequested(false);
        }
    }

    private void callHelpFor(int playerId) {
        Base allyBase = getBase(TEAM_ALLY);
        if (allyBase == null || allyBase.destroyed()) return;
        Tank caller = getTank(playerId);
        if (caller == null || caller.hp <= 0 || caller.team != TEAM_ALLY) return;
        setPlayerHelp(playerId, true, caller.x, caller.y);
        assignReserveHelpers();
    }

    private void stopHelpFor(int playerId) {
        setPlayerHelp(playerId, false, 0f, 0f);
        assignReserveHelpers();
    }

    private void setPlayerHelp(int playerId, boolean active, float x, float y) {
        if (playerId == 1) {
            playerOneHelpActive = active;
            playerOneHelpX = x;
            playerOneHelpY = y;
        } else if (playerId == 2) {
            playerTwoHelpActive = active;
            playerTwoHelpX = x;
            playerTwoHelpY = y;
        }
    }

    private void assignReserveHelpers() {
        Tank p1 = getTank(1);
        Tank p2 = getTank(2);
        if (playerOneHelpActive && p1 != null && p1.hp > 0) {
            playerOneHelpX = p1.x;
            playerOneHelpY = p1.y;
        }
        if (playerTwoHelpActive && p2 != null && p2.hp > 0) {
            playerTwoHelpX = p2.x;
            playerTwoHelpY = p2.y;
        }
        if (!playerOneHelpActive && !playerTwoHelpActive) {
            for (Tank tank : tanks) {
                if (!tank.reserveHelper || tank.team != TEAM_ALLY) continue;
                tank.helpActive = false;
                tank.assignedPlayerId = 0;
            }
            return;
        }
        int helperIndex = 0;
        for (Tank tank : tanks) {
            if (!tank.reserveHelper || tank.team != TEAM_ALLY || tank.hp <= 0) continue;
            int assigned = chooseHelpPlayer(helperIndex);
            if (!tank.helpActive || tank.assignedPlayerId != assigned) {
                tank.detourUntil = 0f;
                tank.stuckTime = 0f;
                tank.lastNavX = tank.x;
                tank.lastNavY = tank.y;
            }
            tank.helpActive = true;
            tank.assignedPlayerId = assigned;
            if (assigned == 1) {
                tank.helpX = playerOneHelpX;
                tank.helpY = playerOneHelpY;
            } else {
                tank.helpX = playerTwoHelpX;
                tank.helpY = playerTwoHelpY;
            }
            helperIndex++;
        }
    }

    private int chooseHelpPlayer(int helperIndex) {
        if (playerOneHelpActive && playerTwoHelpActive) return helperIndex % 2 == 0 ? 1 : 2;
        return playerOneHelpActive ? 1 : 2;
    }

    private void updateReserveHelpers(float dt) {
        if (playerOneHelpActive && (getTank(1) == null || getTank(1).hp <= 0)) playerOneHelpActive = false;
        if (playerTwoHelpActive && (getTank(2) == null || getTank(2).hp <= 0)) playerTwoHelpActive = false;
        assignReserveHelpers();
        assignEnemyReserveHelpers();
    }

    private void updateEnemyHelp(float dt) {
        enemyHelpCooldown = Math.max(0f, enemyHelpCooldown - dt);
        Base enemyBase = getBase(TEAM_ENEMY);
        if (enemyBase == null || enemyBase.destroyed()) {
            enemyHelpActive = false;
            return;
        }

        Tank intruder = nearestEnemyNearPoint(enemyBase.x, enemyBase.y, TEAM_ENEMY, 1450f);
        boolean baseHurting = enemyBase.hp < BASE_MAX_HP * 0.70f;
        if (intruder != null) {
            enemyHelpX = intruder.x;
            enemyHelpY = intruder.y;
            enemyHelpSafeTimer = 0f;
            if (!enemyHelpActive && enemyHelpCooldown <= 0f) {
                enemyHelpActive = true;
            }
        } else if (enemyHelpActive) {
            enemyHelpSafeTimer += dt;
            if (enemyHelpSafeTimer > (baseHurting ? 13f : 7f)) {
                enemyHelpActive = false;
                enemyHelpCooldown = 18f;
                enemyHelpSafeTimer = 0f;
            }
        } else if (baseHurting && enemyHelpCooldown <= 0f) {
            enemyHelpActive = true;
            enemyHelpX = enemyBase.x - 420f;
            enemyHelpY = enemyBase.y - 420f;
        }
    }

    private void assignEnemyReserveHelpers() {
        if (!enemyHelpActive) {
            for (Tank tank : tanks) {
                if (!tank.reserveHelper || tank.team != TEAM_ENEMY) continue;
                tank.helpActive = false;
                tank.assignedPlayerId = 0;
            }
            return;
        }
        for (Tank tank : tanks) {
            if (!tank.reserveHelper || tank.team != TEAM_ENEMY || tank.hp <= 0) continue;
            if (!tank.helpActive) {
                tank.detourUntil = 0f;
                tank.stuckTime = 0f;
                tank.lastNavX = tank.x;
                tank.lastNavY = tank.y;
            }
            tank.helpActive = true;
            tank.assignedPlayerId = -1;
            tank.helpX = enemyHelpX;
            tank.helpY = enemyHelpY;
        }
    }

    private InputState retreatToRepair(Tank bot, Base homeBase, Tank threat, float time) {
        if (threat != null && distance(bot.x, bot.y, threat.x, threat.y) < 560f) {
            InputState fight = attackTank(bot, threat, time, 760f);
            float dx = homeBase.repairX() - bot.x;
            float dy = homeBase.repairY() - bot.y;
            float len = Math.max(1f, (float) Math.hypot(dx, dy));
            fight.moveX = fight.moveX * 0.35f + dx / len;
            fight.moveY = fight.moveY * 0.35f + dy / len;
            return fight;
        }
        float angle = (float) Math.atan2(homeBase.repairY() - bot.y, homeBase.repairX() - bot.x);
        return moveToward(bot, homeBase.repairX(), homeBase.repairY(), angle, false);
    }

    private InputState patrolFront(Tank bot, float time) {
        int slot = Math.abs(bot.id) % 5;
        float dir = teamDir(bot.team);
        float baseX = bot.team == TEAM_ALLY ? 2350f : WORLD_W - 2350f;
        float phase = (float) Math.sin(time * 0.28f + bot.id * 0.91f);
        float px = baseX + dir * phase * 520f;
        float py = 1120f + slot * 860f + (float) Math.sin(time * 0.41f + bot.id) * 190f;
        Tank target = nearestEnemyNearPoint(px, py, bot.team, 780f);
        if (target != null) return attackTank(bot, target, time, 920f);
        Base enemyBase = getBase(bot.team == TEAM_ALLY ? TEAM_ENEMY : TEAM_ALLY);
        if (enemyBase != null && waveAttacking(bot.team, time)) {
            return attackObjective(bot, enemyBase, time);
        }
        float aim = (float) Math.atan2(WORLD_H * 0.5f - bot.y, WORLD_W * 0.5f - bot.x);
        return moveToward(bot, px, py, aim, false);
    }

    private InputState supportFront(Tank bot, float time) {
        float dir = teamDir(bot.team);
        int slot = Math.abs(bot.id) % 4;
        float anchorX = bot.team == TEAM_ALLY ? 2700f : WORLD_W - 2700f;
        float anchorY = 1450f + slot * 930f;
        float swing = (float) Math.sin(time * 0.38f + bot.id * 0.52f);
        float px = anchorX + dir * (360f + swing * 220f);
        float py = anchorY + (float) Math.cos(time * 0.27f + bot.id) * 210f;
        Tank target = nearestEnemyNearPoint(px, py, bot.team, 1020f);
        if (target != null) return attackTank(bot, target, time, 930f);
        Base enemyBase = getBase(bot.team == TEAM_ALLY ? TEAM_ENEMY : TEAM_ALLY);
        if (enemyBase != null && !enemyBase.destroyed() && (waveAttacking(bot.team, time) || distance(bot.x, bot.y, enemyBase.x, enemyBase.y) < 1450f)) {
            return attackObjective(bot, enemyBase, time);
        }
        float aim = dir > 0f ? 0f : (float) Math.PI;
        return moveToward(bot, px, py, aim, false);
    }

    private InputState moveToRally(Tank bot, float time) {
        float dir = teamDir(bot.team);
        int slot = Math.abs(bot.id) % 4;
        float rallyX = bot.team == TEAM_ALLY ? 1880f : WORLD_W - 1880f;
        float rallyY = ASSAULT_LANES[slot % ASSAULT_LANES.length] + (float) Math.sin(time * 0.36f + bot.id) * 120f;
        float aim = dir > 0f ? 0f : (float) Math.PI;
        return moveToward(bot, rallyX, rallyY, aim, false);
    }

    private InputState moveToward(Tank bot, float targetX, float targetY, float aimAngle, boolean shooting) {
        InputState input = new InputState();
        float dx = targetX - bot.x;
        float dy = targetY - bot.y;
        float dist = Math.max(1f, (float) Math.hypot(dx, dy));
        float moveX = dist > 70f ? dx / dist : 0f;
        float moveY = dist > 70f ? dy / dist : 0f;
        float[] move = avoidMove(bot, moveX, moveY, bot.reserveHelper ? 240f : 170f);
        moveX = move[0];
        moveY = move[1];
        input.set(moveX, moveY, (float) Math.cos(aimAngle), (float) Math.sin(aimAngle), shooting);
        return input;
    }

    private float[] avoidMove(Tank bot, float moveX, float moveY, float lookAhead) {
        float len = (float) Math.hypot(moveX, moveY);
        if (len < 0.08f) return new float[]{moveX, moveY};
        moveX /= len;
        moveY /= len;
        if (!blockedForTank(bot.x + moveX * lookAhead, bot.y + moveY * lookAhead, TANK_R + 8f)) {
            return new float[]{moveX, moveY};
        }

        float side = bot.avoidSide;
        float leftX = -moveY * side;
        float leftY = moveX * side;
        float steerX = moveX * 0.38f + leftX;
        float steerY = moveY * 0.38f + leftY;
        float steerLen = Math.max(0.001f, (float) Math.hypot(steerX, steerY));
        steerX /= steerLen;
        steerY /= steerLen;
        if (!blockedForTank(bot.x + steerX * lookAhead, bot.y + steerY * lookAhead, TANK_R + 8f)) {
            return new float[]{steerX, steerY};
        }

        bot.avoidSide = -bot.avoidSide;
        leftX = -moveY * bot.avoidSide;
        leftY = moveX * bot.avoidSide;
        if (!blockedForTank(bot.x + leftX * lookAhead * 0.8f, bot.y + leftY * lookAhead * 0.8f, TANK_R + 8f)) {
            return new float[]{leftX, leftY};
        }
        return new float[]{moveX * -0.35f, moveY * -0.35f};
    }

    private boolean shouldFight(Tank bot, Tank target, Base enemyBase) {
        float dist = distance(bot.x, bot.y, target.x, target.y);
        if (dist < 520f) return true;
        if (bot.role == ROLE_DEFEND || bot.role == ROLE_PATROL) return true;
        return enemyBase == null || distanceToBase(bot, enemyBase) > 820f;
    }

    private Tank nearestEnemyNearPoint(float x, float y, int friendlyTeam, float radius) {
        Tank best = null;
        float bestDist = radius * radius;
        for (Tank tank : tanks) {
            if (tank.hp <= 0 || tank.team == friendlyTeam) continue;
            float d = (tank.x - x) * (tank.x - x) + (tank.y - y) * (tank.y - y);
            if (d < bestDist) {
                bestDist = d;
                best = tank;
            }
        }
        return best;
    }

    private boolean waveAttacking(int team, float time) {
        float offset = team == TEAM_ALLY ? 4f : 0f;
        float phase = (time + offset) % WAVE_SECONDS;
        return phase < WAVE_ATTACK_SECONDS;
    }

    private void updateMegaAttack(float dt) {
        Base ally = getBase(TEAM_ALLY);
        Base enemy = getBase(TEAM_ENEMY);
        if (ally == null || enemy == null || ally.destroyed() || enemy.destroyed()) {
            megaAttackTeam = 0;
            megaAttackTimer = 0f;
            megaBannerTimer = 0f;
            return;
        }

        if (megaAttackTimer > 0f) {
            megaAttackTimer -= dt;
            megaBannerTimer = Math.max(0f, megaBannerTimer - dt);
            if (megaAttackTimer <= 0f) {
                megaAttackTeam = 0;
                megaAttackTimer = 0f;
                megaAttackCooldown = MEGA_ATTACK_INTERVAL;
            }
            return;
        }

        megaAttackCooldown -= dt;
        if (megaAttackCooldown > 0f) return;

        int chosenTeam = random.nextBoolean() ? TEAM_ALLY : TEAM_ENEMY;
        if (ally.hp < BASE_MAX_HP * 0.42f && enemy.hp > ally.hp) {
            chosenTeam = TEAM_ALLY;
        } else if (enemy.hp < BASE_MAX_HP * 0.42f && ally.hp > enemy.hp) {
            chosenTeam = TEAM_ENEMY;
        }

        megaAttackTeam = chosenTeam;
        megaAttackTimer = MEGA_ATTACK_DURATION;
        megaBannerTimer = MEGA_ATTACK_BANNER_SECONDS;
        megaAttackCooldown = MEGA_ATTACK_INTERVAL;
    }

    private boolean megaAttackActiveFor(int team) {
        return megaAttackTeam == team && megaAttackTimer > 0f;
    }

    private boolean megaAttackActiveAgainst(int team) {
        return megaAttackTeam != 0 && megaAttackTeam != team && megaAttackTimer > 0f;
    }

    private static float teamDir(int team) {
        return team == TEAM_ALLY ? 1f : -1f;
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x2 - x1, y2 - y1);
    }

    private static float distanceToBase(Tank tank, Base base) {
        return distance(tank.x, tank.y, base.x, base.y);
    }

    private void updateBullets(float dt) {
        Iterator<Bullet> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            Bullet b = iterator.next();
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            b.life -= dt;
            if (b.life <= 0f || b.x < 0 || b.y < 0 || b.x > WORLD_W || b.y > WORLD_H || hitsObstacle(b.x, b.y, 8f)) {
                iterator.remove();
                continue;
            }
            if (hitBaseWithBullet(b)) {
                iterator.remove();
                continue;
            }
            for (Tank t : tanks) {
                if (t.id == b.owner || t.hp <= 0 || t.team == b.team) continue;
                float d = (float) Math.hypot(t.x - b.x, t.y - b.y);
                if (d < TANK_R + 8f) {
                    t.hp -= BULLET_DAMAGE;
                    if (t.hp <= 0) killTank(t);
                    iterator.remove();
                    break;
                }
            }
        }
    }

    private void updateRepairZones(float dt) {
        for (Base base : bases) {
            if (base.destroyed()) continue;
            for (Tank tank : tanks) {
                if (tank.team != base.team || tank.hp <= 0) continue;
                float d = (float) Math.hypot(tank.x - base.repairX(), tank.y - base.repairY());
                if (d < base.size * 0.62f) {
                    tank.hp = Math.min(TANK_MAX_HP, tank.hp + (int) (46f * dt));
                }
            }
        }
    }

    private void updateBasePressure(float dt) {
        for (Base base : bases) {
            if (base.destroyed()) continue;
            float pressureDps = 0f;
            boolean megaPressure = false;
            for (Tank tank : tanks) {
                if (tank.hp <= 0 || tank.team == base.team) continue;
                float dist = distance(tank.x, tank.y, base.x, base.y);
                boolean tankMega = megaAttackActiveFor(tank.team);
                float pressureRange = tankMega ? 1320f : 1080f;
                float clearShotRange = tankMega ? 860f : 680f;
                if (dist > pressureRange) continue;
                if (dist > clearShotRange && !clearShot(tank.x, tank.y, base.x, base.y)) continue;
                float dps = dist < 620f ? 8.5f : 5.0f;
                if (tankMega) {
                    dps *= MEGA_BASE_PRESSURE_MULTIPLIER;
                    megaPressure = true;
                }
                if (tank.reserveHelper) dps += 1.5f;
                pressureDps += dps;
            }
            if (pressureDps > 0f) {
                float cap = megaPressure ? MEGA_BASE_PRESSURE_DPS_CAP : BASE_PRESSURE_DPS_CAP;
                applyBaseDamage(base, Math.min(pressureDps, cap) * dt);
            }
        }
    }

    private void updateRespawns(float dt) {
        for (Tank tank : tanks) {
            if (tank.hp > 0 || tank.respawnTimer <= 0f) continue;
            tank.respawnTimer -= dt;
            if (tank.respawnTimer <= 0f) {
                Base base = getBase(tank.team);
                if (tank.bot && (base == null || base.destroyed())) continue;
                if (!tank.bot && base != null && base.destroyed()) continue;
                respawn(tank);
            }
        }
    }

    private boolean hitBaseWithBullet(Bullet bullet) {
        for (Base base : bases) {
            if (base.destroyed() || base.team == bullet.team) continue;
            if (base.hitRect.contains(bullet.x, bullet.y)) {
                applyBaseDamage(base, megaAttackActiveFor(bullet.team) ? MEGA_BASE_BULLET_DAMAGE : BASE_BULLET_DAMAGE);
                return true;
            }
        }
        return false;
    }

    private void applyBaseDamage(Base base, float damage) {
        if (base.destroyed() || damage <= 0f) return;
        base.pendingDamage += damage;
        int wholeDamage = (int) base.pendingDamage;
        if (wholeDamage <= 0) return;
        base.pendingDamage -= wholeDamage;
        base.hp = Math.max(0, base.hp - wholeDamage);
    }

    private void updateObstacles(float dt) {
        for (Obstacle obstacle : obstacles) {
            if (!obstacle.falling || obstacle.gone) continue;
            obstacle.fallTime += dt;
            if (obstacle.fallTime > TREE_REMOVE_SECONDS) obstacle.gone = true;
        }
    }

    private void toppleTrees(float x, float y, float r, float bodyAngle) {
        for (Obstacle obstacle : obstacles) {
            if (!obstacle.isTree() || obstacle.falling || obstacle.gone) continue;
            float cx = clamp(x, obstacle.hitRect.left, obstacle.hitRect.right);
            float cy = clamp(y, obstacle.hitRect.top, obstacle.hitRect.bottom);
            if ((x - cx) * (x - cx) + (y - cy) * (y - cy) < r * r) {
                obstacle.falling = true;
                obstacle.blocksShots = false;
                obstacle.solid = false;
                obstacle.fallTime = 0f;
                obstacle.fallSign = Math.cos(bodyAngle) >= 0f ? 1f : -1f;
            }
        }
    }

    private void respawn(Tank t) {
        t.hp = TANK_MAX_HP;
        t.respawnTimer = 0f;
        if (t.reserveHelper) {
            t.x = t.stationX;
            t.y = t.stationY;
            t.detourUntil = 0f;
            t.stuckTime = 0f;
            t.lastNavX = t.x;
            t.lastNavY = t.y;
            t.bodyAngle = -(float) Math.PI * 0.5f;
            t.turretAngle = t.bodyAngle;
        } else if (t.id == 1) {
            t.x = 380f;
            t.y = 380f;
        } else if (t.id == 2) {
            t.x = 560f;
            t.y = 430f;
        } else {
            Base base = getBase(t.team);
            float centerX = base == null ? WORLD_W * 0.5f : base.x;
            float centerY = base == null ? WORLD_H * 0.5f : base.y;
            for (int tries = 0; tries < 80; tries++) {
                float x = centerX + random.nextFloat() * 520f - 260f;
                float y = centerY + random.nextFloat() * 520f - 260f;
                x = clamp(x, TANK_R, WORLD_W - TANK_R);
                y = clamp(y, TANK_R, WORLD_H - TANK_R);
                if (!hitsObstacle(x, y, TANK_R + 20f) && !hitsBase(x, y, TANK_R + 20f)) {
                    t.x = x;
                    t.y = y;
                    break;
                }
            }
        }
    }

    private void killTank(Tank t) {
        t.hp = 0;
        t.respawnTimer = t.bot ? BOT_RESPAWN_SECONDS + (Math.abs(t.id) % 4) * 0.65f : PLAYER_RESPAWN_SECONDS;
        t.reload = 0f;
        t.flash = 0f;
    }

    private Tank nearestEnemy(Tank from, float maxDistance) {
        Tank best = null;
        float bestDist = maxDistance * maxDistance;
        for (Tank t : tanks) {
            if (t == from || t.hp <= 0 || t.team == from.team) continue;
            float d = (t.x - from.x) * (t.x - from.x) + (t.y - from.y) * (t.y - from.y);
            if (d < bestDist) {
                bestDist = d;
                best = t;
            }
        }
        return best;
    }

    private Tank nearestEnemyNearBase(Tank from, Base base, float baseRadius) {
        if (base == null) return nearestEnemy(from, 900f);
        Tank best = null;
        float bestDist = Float.MAX_VALUE;
        float baseRadiusSq = baseRadius * baseRadius;
        for (Tank t : tanks) {
            if (t == from || t.hp <= 0 || t.team == from.team) continue;
            float bd = (t.x - base.x) * (t.x - base.x) + (t.y - base.y) * (t.y - base.y);
            if (bd > baseRadiusSq) continue;
            float d = (t.x - from.x) * (t.x - from.x) + (t.y - from.y) * (t.y - from.y);
            if (d < bestDist) {
                bestDist = d;
                best = t;
            }
        }
        return best;
    }

    private boolean clearShot(float x1, float y1, float x2, float y2) {
        float left = Math.min(x1, x2);
        float top = Math.min(y1, y2);
        float right = Math.max(x1, x2);
        float bottom = Math.max(y1, y2);
        for (Base base : bases) {
            if (base.destroyed()) continue;
            if (base.hitRect.contains(x2, y2)) continue;
            if (base.hitRect.intersects(left, top, right, bottom)) return false;
        }
        for (Obstacle o : obstacles) {
            if (o.gone) continue;
            if (!o.blocksShots) continue;
            if (o.hitRect.intersects(left, top, right, bottom)) return false;
        }
        return true;
    }

    private Tank getTank(int id) {
        for (Tank tank : tanks) if (tank.id == id) return tank;
        return null;
    }

    private Base getBase(int team) {
        for (Base base : bases) if (base.team == team) return base;
        return null;
    }

    private void applyObstacleSnapshot(String snapshot) {
        for (Obstacle obstacle : obstacles) {
            if (!obstacle.isTree()) continue;
            obstacle.falling = false;
            obstacle.gone = false;
            obstacle.solid = false;
            obstacle.blocksShots = false;
            obstacle.fallTime = 0f;
            obstacle.fallSign = 1f;
        }
        for (String item : snapshot.split(";")) {
            if (item.length() < 2) continue;
            String[] p = item.split(",");
            if (p.length < 5 || !"O".equals(p[0])) continue;
            try {
                int index = Integer.parseInt(p[1]);
                if (index < 0 || index >= obstacles.size()) continue;
                Obstacle obstacle = obstacles.get(index);
                if (!obstacle.isTree()) continue;
                obstacle.falling = true;
                obstacle.solid = false;
                obstacle.blocksShots = false;
                obstacle.fallTime = Float.parseFloat(p[2]);
                obstacle.fallSign = Float.parseFloat(p[3]);
                obstacle.gone = "1".equals(p[4]);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void applyBaseSnapshot(String snapshot) {
        for (String item : snapshot.split(";")) {
            if (item.length() < 2) continue;
            String[] p = item.split(",");
            if (p.length < 3 || !"BA".equals(p[0])) continue;
            try {
                Base base = getBase(Integer.parseInt(p[1]));
                if (base != null) {
                    base.hp = Integer.parseInt(p[2]);
                    base.pendingDamage = 0f;
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void applyMegaSnapshot(String snapshot) {
        for (String item : snapshot.split(";")) {
            if (item.length() < 2) continue;
            String[] p = item.split(",");
            if (p.length < 4 || !"M".equals(p[0])) continue;
            try {
                megaAttackTeam = Integer.parseInt(p[1]);
                megaAttackTimer = Float.parseFloat(p[2]);
                megaBannerTimer = Float.parseFloat(p[3]);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private boolean hitsObstacle(float x, float y, float r) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.gone) continue;
            if (!obstacle.solid) continue;
            if (x + r < obstacle.hitRect.left || x - r > obstacle.hitRect.right
                    || y + r < obstacle.hitRect.top || y - r > obstacle.hitRect.bottom) {
                continue;
            }
            float cx = clamp(x, obstacle.hitRect.left, obstacle.hitRect.right);
            float cy = clamp(y, obstacle.hitRect.top, obstacle.hitRect.bottom);
            if ((x - cx) * (x - cx) + (y - cy) * (y - cy) < r * r) return true;
        }
        return false;
    }

    private boolean blockedForTank(float x, float y, float r) {
        return hitsObstacle(x, y, r) || hitsBase(x, y, r);
    }

    private int ownerTeam(int ownerId) {
        Tank owner = getTank(ownerId);
        return owner == null ? TEAM_ENEMY : owner.team;
    }

    private boolean hitsBase(float x, float y, float r) {
        for (Base base : bases) {
            if (base.destroyed()) continue;
            if (x + r < base.hitRect.left || x - r > base.hitRect.right
                    || y + r < base.hitRect.top || y - r > base.hitRect.bottom) {
                continue;
            }
            float cx = clamp(x, base.hitRect.left, base.hitRect.right);
            float cy = clamp(y, base.hitRect.top, base.hitRect.bottom);
            if ((x - cx) * (x - cx) + (y - cy) * (y - cy) < r * r) return true;
        }
        return false;
    }

    private void buildMap() {
        obstacles.clear();
        roads.clear();
        scars.clear();
        cracks.clear();
        roadMarks.clear();
        buildRoads();
        buildScars();
        buildCracks();
        buildRoadMarks();
        Random map = new Random(421);
        buildDefensiveBarriers(map);
        buildForestClusters(map);
        buildTallGrassPatches(map);
        buildBattleClutter(map);
        buildSupplyFields(map);
        for (int i = 0; i < 38; i++) {
            int kind = pickProp(map);
            float x = 520f + map.nextFloat() * (WORLD_W - 1040f);
            float y = 520f + map.nextFloat() * (WORLD_H - 1040f);
            placePropCentered(kind, x, y, map, kind == BUSH ? 6f : 48f);
        }
        for (int i = 0; i < 46; i++) {
            float x = 360f + map.nextFloat() * (WORLD_W - 720f);
            float y = 360f + map.nextFloat() * (WORLD_H - 720f);
            placePropCentered(CRATER, x, y, map, 0f);
        }
        placeWallLine(1660f, 1540f, 3, true);
        placeWallLine(3560f, 2820f, 2, false);
        addFixedProp(LAKE, 3320, 2660, 3760, 3060);
        placeFenceLine(4550f, 1020f, 2, true);
        addFixedProp(BUSH, 900, 1350, 1080, 1530);
        placeBarrelStack(1160f, 1540f);
        placePropCentered(WATCHTOWER, 1120f, 900f, map, 60f);
        placePropCentered(WATCHTOWER, WORLD_W - 1120f, WORLD_H - 900f, map, 60f);
        placePropCentered(DESTROYED_TANK, 2470f, 1160f, map, 70f);
        placePropCentered(DESTROYED_TANK, 3870f, 4920f, map, 70f);
    }

    private void buildForestClusters(Random map) {
        float[][] centers = {
                {680f, 1780f}, {2060f, 760f}, {3860f, 1600f},
                {860f, 4210f}, {2480f, 4450f}, {5380f, 2550f}, {5200f, 4380f}
        };
        for (float[] center : centers) {
            int count = 16 + map.nextInt(11);
            for (int i = 0; i < count; i++) {
                float angle = map.nextFloat() * (float) Math.PI * 2f;
                float radius = (float) Math.sqrt(map.nextFloat()) * (280f + map.nextInt(260));
                float x = center[0] + (float) Math.cos(angle) * radius;
                float y = center[1] + (float) Math.sin(angle) * radius;
                placePropCentered(map.nextBoolean() ? TREE_1 : TREE_2, x, y, map, 90f);
            }
            for (int i = 0; i < 7; i++) {
                float angle = map.nextFloat() * (float) Math.PI * 2f;
                float radius = 120f + map.nextFloat() * 420f;
                float x = center[0] + (float) Math.cos(angle) * radius;
                float y = center[1] + (float) Math.sin(angle) * radius;
                placePropCentered(BUSH, x, y, map, 8f);
            }
        }
    }

    private void buildTallGrassPatches(Random map) {
        float[][] centers = {
                {760f, 1460f}, {980f, 2160f}, {720f, 2450f}, {1480f, 3940f}, {2320f, 1240f}, {2780f, 5340f},
                {4020f, 850f}, {4380f, 4560f}, {5480f, 1780f}, {5520f, 3880f}
        };
        for (float[] center : centers) {
            int count = 14 + map.nextInt(8);
            for (int i = 0; i < count; i++) {
                float angle = map.nextFloat() * (float) Math.PI * 2f;
                float radius = (float) Math.sqrt(map.nextFloat()) * (220f + map.nextInt(310));
                float x = center[0] + (float) Math.cos(angle) * radius;
                float y = center[1] + (float) Math.sin(angle) * radius;
                placeTallGrass(x, y, map);
            }
        }
    }

    private void buildDefensiveBarriers(Random map) {
        buildFenceBarrier(1500f, 760f, WORLD_H - 820f, map, 1f);
        buildFenceBarrier(WORLD_W - 1630f, 760f, WORLD_H - 820f, map, -1f);
    }

    private void buildFenceBarrier(float baseX, float startY, float endY, Random map, float side) {
        float[] gateY = {1120f, 3115f, 4970f};
        for (float y = startY; y < endY; y += 238f) {
            if (nearGate(y, gateY, 260f)) continue;
            if (map.nextInt(100) < 12) continue;
            float wave = (float) Math.sin(y * 0.006f + side * 1.7f) * 52f;
            float jitter = map.nextInt(55) - 27f;
            float x = baseX + wave + jitter;
            placePropRect(FENCE, x, y + map.nextInt(30) - 15f, x + 112f, y + 198f + map.nextInt(22), 24f);
            if (map.nextInt(100) < 24) {
                float sideX = x + side * (104f + map.nextInt(42));
                placePropRect(FENCE, sideX, y + 54f, sideX + 112f, y + 236f, 24f);
            }
        }

        for (float gate : gateY) {
            float wave = (float) Math.sin(gate * 0.006f + side * 1.7f) * 52f;
            float x = baseX + wave;
            placePropCentered(WATCHTOWER, x - side * 245f, gate - 185f, map, 30f);
            placePropCentered(WATCHTOWER, x + side * 260f, gate + 190f, map, 30f);
            placePropCentered(SANDBAGS, x - side * 170f, gate + 20f, map, 20f);
            placePropCentered(CONCRETE_BARRIER, x + side * 165f, gate - 20f, map, 20f);
        }
    }

    private boolean nearGate(float y, float[] gates, float radius) {
        for (float gate : gates) {
            if (Math.abs(y - gate) < radius) return true;
        }
        return false;
    }

    private void buildBattleClutter(Random map) {
        float[][] camps = {
                {1640f, 1580f}, {3420f, 2860f}, {4720f, 1060f}, {4300f, 4050f},
                {2200f, 3320f}, {980f, 2620f}, {5440f, 3440f}, {3200f, 5200f}
        };
        for (float[] camp : camps) {
            placeWallLine(camp[0] - 120f, camp[1], 2 + map.nextInt(2), map.nextBoolean());
            placeFenceLine(camp[0] + 120f, camp[1] + 120f, 1 + map.nextInt(2), map.nextBoolean());
            placeBarrelStack(camp[0] - 70f + map.nextInt(140), camp[1] - 70f + map.nextInt(140));
            placePropNear(SANDBAGS, camp[0] - 150f, camp[1] + 170f, map, 150f, 36f, 10);
            placePropNear(CONCRETE_BARRIER, camp[0] + 210f, camp[1] - 110f, map, 160f, 36f, 10);
            placePropNear(AMMO_CRATE, camp[0] - 40f, camp[1] - 210f, map, 150f, 36f, 10);
            placePropNear(FUEL_CANISTERS, camp[0] + 190f, camp[1] + 210f, map, 150f, 36f, 10);
            if (map.nextBoolean()) {
                placePropNear(DESTROYED_TANK, camp[0] + 330f, camp[1] + 20f, map, 180f, 58f, 10);
            }
        }
    }

    private void buildSupplyFields(Random map) {
        float[][] fields = {
                {1040f, 3840f}, {1840f, 2440f}, {2760f, 1760f}, {3920f, 3680f},
                {5020f, 1760f}, {5220f, 5200f}, {1460f, 5200f}, {3740f, 880f}
        };
        for (float[] field : fields) {
            placePropNear(AMMO_CRATE, field[0] - 120f, field[1] - 40f, map, 210f, 36f, 14);
            placePropNear(FUEL_CANISTERS, field[0] + 140f, field[1] + 80f, map, 210f, 36f, 14);
            placePropNear(SANDBAGS, field[0] - 180f, field[1] + 190f, map, 230f, 38f, 14);
            placePropNear(CONCRETE_BARRIER, field[0] + 210f, field[1] - 160f, map, 230f, 38f, 14);
            if (map.nextInt(100) < 70) {
                placePropNear(WATCHTOWER, field[0] + 20f, field[1] - 300f, map, 240f, 55f, 12);
            }
        }
    }

    private boolean placePropNear(int kind, float centerX, float centerY, Random map, float spread, float roadPadding, int tries) {
        for (int i = 0; i < tries; i++) {
            float angle = map.nextFloat() * (float) Math.PI * 2f;
            float radius = (float) Math.sqrt(map.nextFloat()) * spread;
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;
            if (placePropCentered(kind, x, y, map, roadPadding)) {
                return true;
            }
        }
        return false;
    }

    private void placeWallLine(float x, float y, int count, boolean horizontal) {
        for (int i = 0; i < count; i++) {
            float l = x + (horizontal ? i * 176f : 0f);
            float t = y + (horizontal ? 0f : i * 176f);
            addFixedProp(WALL, l, t, l + 166f, t + 166f);
        }
    }

    private void placeFenceLine(float x, float y, int count, boolean horizontal) {
        for (int i = 0; i < count; i++) {
            float l = x + (horizontal ? i * 236f : 0f);
            float t = y + (horizontal ? 0f : i * 116f);
            addFixedProp(FENCE, l, t, l + (horizontal ? 220f : 108f), t + (horizontal ? 104f : 220f));
        }
    }

    private void placeBarrelStack(float x, float y) {
        addFixedProp(BARREL, x, y, x + 96f, y + 96f);
        addFixedProp(BARREL, x + 82f, y + 18f, x + 178f, y + 114f);
        addFixedProp(BARREL, x + 26f, y + 86f, x + 122f, y + 182f);
    }

    private boolean placeProp(int kind, float x, float y, Random map, float roadPadding) {
        float w = propWidth(kind, map);
        float h = propHeight(kind, map);
        RectF candidate = new RectF(x, y, x + w, y + h);
        if (!canPlace(kind, candidate, roadPadding)) return false;
        obstacles.add(new Obstacle(kind, candidate.left, candidate.top, candidate.right, candidate.bottom,
                isSolidProp(kind),
                blocksShotsProp(kind)));
        return true;
    }

    private boolean placePropCentered(int kind, float centerX, float centerY, Random map, float roadPadding) {
        float w = propWidth(kind, map);
        float h = propHeight(kind, map);
        return placePropRect(kind, centerX - w * 0.5f, centerY - h * 0.5f, centerX + w * 0.5f, centerY + h * 0.5f, roadPadding);
    }

    private boolean placeTallGrass(float centerX, float centerY, Random map) {
        float w = propWidth(TALL_GRASS, map);
        float h = propHeight(TALL_GRASS, map);
        RectF candidate = new RectF(centerX - w * 0.5f, centerY - h * 0.5f, centerX + w * 0.5f, centerY + h * 0.5f);
        if (candidate.left < 140f || candidate.top < 140f || candidate.right > WORLD_W - 140f || candidate.bottom > WORLD_H - 140f) return false;
        if (touchesRoad(candidate, 155f) || hitsBase(candidate.centerX(), candidate.centerY(), Math.max(w, h) * 0.34f)) return false;
        RectF padded = new RectF(candidate.left - 18f, candidate.top - 18f, candidate.right + 18f, candidate.bottom + 18f);
        for (Obstacle obstacle : obstacles) {
            if (obstacle.gone || obstacle.kind == TALL_GRASS || obstacle.kind == BUSH || obstacle.kind == CRATER) continue;
            if (RectF.intersects(padded, obstacle.hitRect)) return false;
        }
        obstacles.add(new Obstacle(TALL_GRASS, candidate.left, candidate.top, candidate.right, candidate.bottom, false, false));
        return true;
    }

    private boolean placePropRect(int kind, float l, float t, float r, float b, float roadPadding) {
        RectF candidate = new RectF(l, t, r, b);
        if (!canPlace(kind, candidate, roadPadding)) return false;
        obstacles.add(new Obstacle(kind, candidate.left, candidate.top, candidate.right, candidate.bottom,
                isSolidProp(kind),
                blocksShotsProp(kind)));
        return true;
    }

    private void addFixedProp(int kind, float l, float t, float r, float b) {
        placePropRect(kind, l, t, r, b, kind == BUSH ? 6f : 48f);
    }

    private boolean canPlace(int kind, RectF candidate, float roadPadding) {
        if (candidate.left < 140f || candidate.top < 140f || candidate.right > WORLD_W - 140f || candidate.bottom > WORLD_H - 140f) return false;
        if ((candidate.left < 930f && candidate.top < 930f) || (candidate.right > WORLD_W - 930f && candidate.bottom > WORLD_H - 930f)) return false;
        if (candidate.intersects(120f, 500f, 850f, 1040f)) return false;
        if (candidate.intersects(WORLD_W - 900f, WORLD_H - 900f, WORLD_W - 120f, WORLD_H - 120f)) return false;
        if (avoidsRoad(kind) && touchesRoad(candidate, roadPadding)) return false;
        RectF check = kind == TREE_1 || kind == TREE_2 ? Obstacle.makeHitRect(kind, candidate) : candidate;
        float gap = kind == TREE_1 || kind == TREE_2 ? 30f : 24f;
        RectF padded = new RectF(check.left - gap, check.top - gap, check.right + gap, check.bottom + gap);
        for (Obstacle obstacle : obstacles) {
            if (obstacle.gone) continue;
            RectF other = obstacle.isTree() ? obstacle.hitRect : obstacle.rect;
            if (RectF.intersects(padded, other)) return false;
            float spacing = sameKindSpacing(kind);
            if (spacing > 0f && obstacle.kind == kind) {
                float dx = check.centerX() - other.centerX();
                float dy = check.centerY() - other.centerY();
                if (dx * dx + dy * dy < spacing * spacing) return false;
            }
        }
        return true;
    }

    private void buildRoads() {
        addRoad(-80f, 1040f, WORLD_W + 80f, 1240f);
        addRoad(-80f, 3000f, WORLD_W + 80f, 3230f);
        addRoad(-80f, 4880f, WORLD_W + 80f, 5060f);
        addRoad(1160f, -80f, 1370f, WORLD_H + 80f);
        addRoad(2910f, -80f, 3140f, WORLD_H + 80f);
        addRoad(4710f, -80f, 4900f, WORLD_H + 80f);
        addRoad(520f, 520f, 1320f, 720f);
        addRoad(WORLD_W - 1320f, WORLD_H - 720f, WORLD_W - 520f, WORLD_H - 520f);
    }

    private void addRoad(float l, float t, float r, float b) {
        roads.add(new RectF(l, t, r, b));
    }

    private void buildScars() {
        Random map = new Random(9044);
        for (RectF road : roads) {
            int count = Math.max(3, (int) ((road.width() + road.height()) / 1700f));
            for (int i = 0; i < count; i++) {
                float x = road.left + map.nextFloat() * road.width();
                float y = road.top + map.nextFloat() * road.height();
                float w = 72f + map.nextFloat() * 180f;
                float h = 22f + map.nextFloat() * 70f;
                scars.add(new Scar(x, y, w, h, map.nextFloat() * 180f, map.nextBoolean() ? 0x56513c27 : 0x4a655139));
            }
        }
        for (int i = 0; i < 36; i++) {
            float x = 260f + map.nextFloat() * (WORLD_W - 520f);
            float y = 260f + map.nextFloat() * (WORLD_H - 520f);
            float w = 48f + map.nextFloat() * 150f;
            float h = 30f + map.nextFloat() * 94f;
            int color = map.nextInt(100) < 38 ? 0x46543e25 : 0x3d4b5635;
            scars.add(new Scar(x, y, w, h, map.nextFloat() * 180f, color));
        }
    }

    private void buildCracks() {
        Random map = new Random(5150);
        for (RectF road : roads) {
            int count = Math.max(2, (int) ((road.width() + road.height()) / 1900f));
            for (int i = 0; i < count; i++) {
                boolean horizontal = road.width() >= road.height();
                float x = road.left + 30f + map.nextFloat() * Math.max(1f, road.width() - 60f);
                float y = road.top + 30f + map.nextFloat() * Math.max(1f, road.height() - 60f);
                float length = 90f + map.nextFloat() * 170f;
                float angle = horizontal ? map.nextFloat() * 24f - 12f : 90f + map.nextFloat() * 24f - 12f;
                cracks.add(new Crack(x, y, length, angle, 0x96312620, 5f, true));
            }
        }
        for (int i = 0; i < 28; i++) {
            float x = 340f + map.nextFloat() * (WORLD_W - 680f);
            float y = 340f + map.nextFloat() * (WORLD_H - 680f);
            float length = 70f + map.nextFloat() * 150f;
            float angle = map.nextFloat() * 180f;
            cracks.add(new Crack(x, y, length, angle, 0x71302118, 3.5f, map.nextBoolean()));
        }
    }

    private void buildRoadMarks() {
        Random map = new Random(944);
        for (RectF road : roads) {
            boolean horizontal = road.width() >= road.height();
            float area = road.width() * road.height();
            int count = Math.max(5, (int) (area / 54000f));
            for (int i = 0; i < count; i++) {
                float x = road.left + 24f + map.nextFloat() * Math.max(1f, road.width() - 48f);
                float y = road.top + 24f + map.nextFloat() * Math.max(1f, road.height() - 48f);
                int roll = map.nextInt(100);
                int kind = roll < 42 ? RoadMark.TRACKS : (roll < 72 ? RoadMark.CRACK : RoadMark.DIRT);
                float angle = horizontal ? (map.nextFloat() * 12f - 6f) : 90f + (map.nextFloat() * 12f - 6f);
                float w;
                float h;
                int color;
                if (kind == RoadMark.TRACKS) {
                    w = 145f + map.nextInt(150);
                    h = 42f + map.nextInt(16);
                    color = 0x55342720;
                } else if (kind == RoadMark.CRACK) {
                    w = 70f + map.nextInt(90);
                    h = 22f + map.nextInt(18);
                    color = 0x88302620;
                } else {
                    w = 90f + map.nextInt(150);
                    h = 46f + map.nextInt(74);
                    angle = map.nextFloat() * 180f;
                    color = map.nextBoolean() ? 0x4f46351f : 0x454f442c;
                }
                roadMarks.add(new RoadMark(kind, x, y, w, h, angle, color));
            }
        }
    }

    private boolean touchesRoad(RectF rect, float padding) {
        RectF padded = new RectF(rect.left - padding, rect.top - padding, rect.right + padding, rect.bottom + padding);
        for (RectF road : roads) {
            if (RectF.intersects(padded, road)) return true;
        }
        return false;
    }

    private int pickProp(Random map) {
        int roll = map.nextInt(100);
        if (roll < 26) return BUSH;
        if (roll < 40) return BARREL;
        if (roll < 53) return FENCE;
        if (roll < 66) return WALL;
        if (roll < 76) return SANDBAGS;
        if (roll < 84) return CONCRETE_BARRIER;
        if (roll < 90) return AMMO_CRATE;
        if (roll < 95) return FUEL_CANISTERS;
        return LAKE;
    }

    private static boolean isSolidProp(int kind) {
        return kind != BUSH && kind != TREE_1 && kind != TREE_2 && kind != CRATER && kind != TALL_GRASS;
    }

    private static boolean blocksShotsProp(int kind) {
        return kind == WALL
                || kind == BARREL
                || kind == FENCE
                || kind == CONCRETE_BARRIER
                || kind == DESTROYED_TANK
                || kind == FUEL_CANISTERS
                || kind == SANDBAGS
                || kind == WATCHTOWER;
    }

    private static boolean avoidsRoad(int kind) {
        return kind != BUSH && kind != CRATER;
    }

    private static float sameKindSpacing(int kind) {
        switch (kind) {
            case AMMO_CRATE:
            case FUEL_CANISTERS:
                return 360f;
            case SANDBAGS:
            case CONCRETE_BARRIER:
                return 420f;
            case DESTROYED_TANK:
                return 700f;
            case WATCHTOWER:
                return 950f;
            case CRATER:
                return 260f;
            case TALL_GRASS:
                return 230f;
            default:
                return 0f;
        }
    }

    private float propWidth(int kind, Random map) {
        switch (kind) {
            case WALL:
                return 150f + map.nextInt(45);
            case BARREL:
                return 88f + map.nextInt(28);
            case FENCE:
                return 178f + map.nextInt(56);
            case BUSH:
                return 150f + map.nextInt(80);
            case LAKE:
                return 310f + map.nextInt(190);
            case TREE_1:
                return 182f + map.nextInt(42);
            case TREE_2:
                return 170f + map.nextInt(38);
            case AMMO_CRATE:
                return 120f + map.nextInt(28);
            case CONCRETE_BARRIER:
                return 220f + map.nextInt(70);
            case CRATER:
                return 118f + map.nextInt(54);
            case TALL_GRASS:
                return 138f + map.nextInt(46);
            case DESTROYED_TANK:
                return 210f + map.nextInt(70);
            case FUEL_CANISTERS:
                return 130f + map.nextInt(38);
            case SANDBAGS:
                return 210f + map.nextInt(90);
            case WATCHTOWER:
                return 190f + map.nextInt(45);
            default:
                return 160f;
        }
    }

    private float propHeight(int kind, Random map) {
        switch (kind) {
            case WALL:
                return 150f + map.nextInt(45);
            case BARREL:
                return 88f + map.nextInt(28);
            case FENCE:
                return 88f + map.nextInt(24);
            case BUSH:
                return 140f + map.nextInt(70);
            case LAKE:
                return 240f + map.nextInt(150);
            case TREE_1:
                return 273f + map.nextInt(64);
            case TREE_2:
                return 255f + map.nextInt(58);
            case AMMO_CRATE:
                return 110f + map.nextInt(28);
            case CONCRETE_BARRIER:
                return 105f + map.nextInt(35);
            case CRATER:
                return 96f + map.nextInt(48);
            case TALL_GRASS:
                return 128f + map.nextInt(42);
            case DESTROYED_TANK:
                return 150f + map.nextInt(55);
            case FUEL_CANISTERS:
                return 130f + map.nextInt(38);
            case SANDBAGS:
                return 105f + map.nextInt(45);
            case WATCHTOWER:
                return 230f + map.nextInt(70);
            default:
                return 160f;
        }
    }

    private void drawTerrain(Canvas canvas, float camX, float camY, int width, int height) {
        if (grassTexture == null) {
            paint.setColor(0xff24381f);
            canvas.drawRect(0, 0, WORLD_W, WORLD_H, paint);
            return;
        }
        float left = Math.max(0f, (float) Math.floor(camX / TERRAIN_TILE) * TERRAIN_TILE);
        float top = Math.max(0f, (float) Math.floor(camY / TERRAIN_TILE) * TERRAIN_TILE);
        float right = Math.min(WORLD_W, camX + width + TERRAIN_TILE);
        float bottom = Math.min(WORLD_H, camY + height + TERRAIN_TILE);
        for (float y = top; y < bottom; y += TERRAIN_TILE) {
            for (float x = left; x < right; x += TERRAIN_TILE) {
                drawRect.set(x, y, Math.min(x + TERRAIN_TILE, WORLD_W), Math.min(y + TERRAIN_TILE, WORLD_H));
                bitmapPaint.setAlpha(255);
                canvas.drawBitmap(grassTexture, null, drawRect, bitmapPaint);
                int shade = terrainTileShade(x, y);
                paint.setColor(shade);
                canvas.drawRect(drawRect, paint);
                paint.setColor(0x20202a1c);
                canvas.drawRect(drawRect, paint);
            }
        }
    }

    private int terrainTileShade(float x, float y) {
        int hx = (int) (x / TERRAIN_TILE);
        int hy = (int) (y / TERRAIN_TILE);
        int hash = Math.abs(hx * 83492791 ^ hy * 297121507);
        int alpha = 10 + hash % 28;
        int tone = hash % 4;
        if (tone == 0) return (alpha << 24) | 0x122011;
        if (tone == 1) return (alpha << 24) | 0x3d3422;
        if (tone == 2) return (alpha << 24) | 0x24351d;
        return (alpha << 24) | 0x4a4b2f;
    }

    private void drawRoads(Canvas canvas, float camX, float camY, int width, int height) {
        if (roadTexture == null) return;
        for (RectF road : roads) {
            if (!RectF.intersects(visibleRect, road)) continue;
            float left = (float) Math.floor(road.left / ROAD_TILE) * ROAD_TILE;
            float top = (float) Math.floor(road.top / ROAD_TILE) * ROAD_TILE;
            for (float y = top; y < road.bottom; y += ROAD_TILE) {
                for (float x = left; x < road.right; x += ROAD_TILE) {
                    drawRect.set(Math.max(x, road.left), Math.max(y, road.top), Math.min(x + ROAD_TILE, road.right), Math.min(y + ROAD_TILE, road.bottom));
                    if (RectF.intersects(visibleRect, drawRect)) {
                        bitmapPaint.setAlpha(255);
                        canvas.drawBitmap(roadTexture, null, drawRect, bitmapPaint);
                        int shade = roadTileShade(x, y);
                        if (shade != 0) {
                            paint.setColor(shade);
                            canvas.drawRect(drawRect, paint);
                        }
                        paint.setColor(0x2032251a);
                        canvas.drawRect(drawRect, paint);
                    }
                }
            }
        }
    }

    private int roadTileShade(float x, float y) {
        int hx = (int) (x / ROAD_TILE);
        int hy = (int) (y / ROAD_TILE);
        int hash = Math.abs(hx * 73856093 ^ hy * 19349663);
        int alpha = 16 + hash % 22;
        int tone = hash % 3;
        if (tone == 0) return (alpha << 24) | 0x2c261c;
        if (tone == 1) return (alpha << 24) | 0x554b3a;
        return (alpha << 24) | 0x1d2520;
    }

    private void drawScars(Canvas canvas, float camX, float camY, int width, int height) {
        paint.setStyle(Paint.Style.FILL);
        for (Scar scar : scars) {
            tempRect.set(scar.x - scar.w * 0.5f, scar.y - scar.h * 0.5f, scar.x + scar.w * 0.5f, scar.y + scar.h * 0.5f);
            if (!RectF.intersects(visibleRect, tempRect)) continue;
            paint.setColor(scar.color);
            canvas.save();
            canvas.rotate(scar.angle, scar.x, scar.y);
            canvas.drawOval(tempRect, paint);
            canvas.restore();
        }
    }

    private void drawCracks(Canvas canvas, float camX, float camY, int width, int height) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (Crack crack : cracks) {
            float half = crack.length * 0.55f;
            tempRect.set(crack.x - half, crack.y - half, crack.x + half, crack.y + half);
            if (!RectF.intersects(visibleRect, tempRect)) continue;
            drawCrack(canvas, crack.x, crack.y, crack.length, crack.angle, crack.color, crack.stroke, crack.branch);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCrack(Canvas canvas, float x, float y, float length, float angle, int color, float stroke, boolean branch) {
        canvas.save();
        canvas.rotate(angle, x, y);
        paint.setColor(color);
        paint.setStrokeWidth(stroke);
        float left = x - length * 0.5f;
        float p1 = x - length * 0.22f;
        float p2 = x + length * 0.08f;
        float p3 = x + length * 0.31f;
        float right = x + length * 0.5f;
        canvas.drawLine(left, y, p1, y - length * 0.07f, paint);
        canvas.drawLine(p1, y - length * 0.07f, p2, y + length * 0.05f, paint);
        canvas.drawLine(p2, y + length * 0.05f, p3, y - length * 0.04f, paint);
        canvas.drawLine(p3, y - length * 0.04f, right, y + length * 0.02f, paint);
        if (branch) {
            paint.setStrokeWidth(Math.max(2f, stroke * 0.68f));
            canvas.drawLine(p2, y + length * 0.05f, p2 + length * 0.16f, y + length * 0.20f, paint);
            canvas.drawLine(p1, y - length * 0.07f, p1 - length * 0.14f, y - length * 0.18f, paint);
        }
        canvas.restore();
    }

    private void drawRoadMarks(Canvas canvas, float camX, float camY, int width, int height) {
        for (RoadMark mark : roadMarks) {
            tempRect.set(mark.x - mark.w * 0.5f, mark.y - mark.h * 0.5f, mark.x + mark.w * 0.5f, mark.y + mark.h * 0.5f);
            if (!RectF.intersects(visibleRect, tempRect)) continue;
            canvas.save();
            canvas.rotate(mark.angle, mark.x, mark.y);
            paint.setColor(mark.color);
            if (mark.kind == RoadMark.TRACKS) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(8f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                float gap = mark.h * 0.36f;
                canvas.drawLine(mark.x - mark.w * 0.5f, mark.y - gap, mark.x + mark.w * 0.5f, mark.y - gap, paint);
                canvas.drawLine(mark.x - mark.w * 0.5f, mark.y + gap, mark.x + mark.w * 0.5f, mark.y + gap, paint);
                paint.setStrokeCap(Paint.Cap.BUTT);
                paint.setStyle(Paint.Style.FILL);
            } else if (mark.kind == RoadMark.CRACK) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                float left = mark.x - mark.w * 0.5f;
                float q1 = mark.x - mark.w * 0.16f;
                float q2 = mark.x + mark.w * 0.18f;
                float right = mark.x + mark.w * 0.5f;
                canvas.drawLine(left, mark.y, q1, mark.y - mark.h * 0.35f, paint);
                canvas.drawLine(q1, mark.y - mark.h * 0.35f, q2, mark.y + mark.h * 0.28f, paint);
                canvas.drawLine(q2, mark.y + mark.h * 0.28f, right, mark.y - mark.h * 0.12f, paint);
                paint.setStrokeWidth(3f);
                canvas.drawLine(q1, mark.y - mark.h * 0.35f, q1 - mark.w * 0.16f, mark.y - mark.h * 0.68f, paint);
                canvas.drawLine(q2, mark.y + mark.h * 0.28f, q2 + mark.w * 0.14f, mark.y + mark.h * 0.62f, paint);
                paint.setStrokeCap(Paint.Cap.BUTT);
                paint.setStyle(Paint.Style.FILL);
            } else {
                canvas.drawOval(tempRect, paint);
            }
            canvas.restore();
        }
    }

    private void drawWorldBounds(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(24f);
        paint.setColor(0xff3a4452);
        canvas.drawRect(0, 0, WORLD_W, WORLD_H, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawHelpStation(Canvas canvas, float x, float y, String label, boolean active, boolean ally) {
        tempRect.set(x - 180f, y - 130f, x + 180f, y + 130f);
        if (!RectF.intersects(visibleRect, tempRect)) return;
        float w = 310f;
        float h = 210f;
        tempRect.set(x - w * 0.5f, y - h * 0.5f, x + w * 0.5f, y + h * 0.5f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xcc1f2937);
        canvas.drawRoundRect(tempRect, 10f, 10f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(active ? 0xfff59e0b : (ally ? 0xff22c55e : 0xffef4444));
        canvas.drawRoundRect(tempRect, 10f, 10f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(24f);
        paint.setColor(0xffffffff);
        canvas.drawText(label, x, y - 72f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawHud(Canvas canvas, int width, int height, float x, float y) {
        Base allyBase = getBase(TEAM_ALLY);
        Base enemyBase = getBase(TEAM_ENEMY);
        drawMinimap(canvas, width, height, x, y, allyBase, enemyBase);
        paint.setTextSize(24f);
        paint.setColor(Color.WHITE);
        Base ally = allyBase;
        Base enemy = enemyBase;
        int allyHp = ally == null ? 0 : ally.hp;
        int enemyHp = enemy == null ? 0 : enemy.hp;
        canvas.drawText("ALLY BASE " + allyHp + "   ENEMY BASE " + enemyHp, 18f, height - 18f, paint);
        paint.setTextSize(22f);
        paint.setColor(0xffdbeafe);
        canvas.drawText(battleStatus(), 18f, height - 48f, paint);
        Tank local = getTank(localPlayerId);
        if (enemy != null && enemy.destroyed()) {
            drawCenterMessage(canvas, width, height, "VICTORY");
        } else if (ally != null && ally.destroyed()) {
            drawCenterMessage(canvas, width, height, "BASE LOST");
        } else if (local != null && local.hp <= 0 && local.respawnTimer > 0f) {
            drawCenterMessage(canvas, width, height, "RESPAWN " + Math.max(1, (int) Math.ceil(local.respawnTimer)));
        } else if (megaBannerTimer > 0f && megaAttackTeam != 0) {
            drawCenterMessage(canvas, width, height, megaAttackTeam == TEAM_ALLY ? "ALLY MEGA ATTACK" : "ENEMY MEGA ATTACK");
        }
    }

    private String battleStatus() {
        Base ally = getBase(TEAM_ALLY);
        Base enemy = getBase(TEAM_ENEMY);
        if (megaAttackTeam == TEAM_ALLY && megaAttackTimer > 0f) {
            return "ALLY MEGA ATTACK";
        }
        if (megaAttackTeam == TEAM_ENEMY && megaAttackTimer > 0f) {
            return "DEFEND: ENEMY MEGA ATTACK";
        }
        if (ally != null && !ally.destroyed() && nearestEnemyNearPoint(ally.x, ally.y, TEAM_ALLY, 930f) != null) {
            return "DEFEND BASE";
        }
        if (enemy != null && !enemy.destroyed() && nearestEnemyNearPoint(enemy.x, enemy.y, TEAM_ENEMY, 930f) != null) {
            return "ALLY PUSH";
        }
        return waveAttacking(TEAM_ENEMY, aiClock) ? "ENEMY WAVE" : "REGROUP";
    }

    private void drawMinimap(Canvas canvas, int width, int height, float x, float y, Base allyBase, Base enemyBase) {
        float pad = 18f;
        float mapLeft = width - MINIMAP_SIZE - pad;
        float mapTop = height - MINIMAP_SIZE - pad;
        float mapRight = width - pad;
        float mapBottom = height - pad;
        tempRect.set(mapLeft - 5f, mapTop - 5f, mapRight + 5f, mapBottom + 5f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff15191e);
        canvas.drawRoundRect(tempRect, 8f, 8f, paint);
        paint.setColor(0xff56616e);
        canvas.drawRoundRect(mapLeft - 2f, mapTop - 2f, mapRight + 2f, mapBottom + 2f, 7f, 7f, paint);
        tempRect.set(mapLeft, mapTop, mapRight, mapBottom);
        paint.setColor(0xb4141b22);
        canvas.drawRoundRect(tempRect, 5f, 5f, paint);
        paint.setColor(0x332f3a46);
        canvas.drawLine(mapLeft + 8f, mapTop + 8f, mapRight - 8f, mapTop + 8f, paint);

        for (RectF road : roads) {
            float l = mapLeft + (road.left / WORLD_W) * MINIMAP_SIZE;
            float t = mapTop + (road.top / WORLD_H) * MINIMAP_SIZE;
            float r = mapLeft + (road.right / WORLD_W) * MINIMAP_SIZE;
            float b = mapTop + (road.bottom / WORLD_H) * MINIMAP_SIZE;
            paint.setColor(0x665e5548);
            canvas.drawRect(l, t, r, b, paint);
        }

        for (Tank tank : tanks) {
            if (tank.hp <= 0) continue;
            float sx = mapLeft + (tank.x / WORLD_W) * MINIMAP_SIZE;
            float sy = mapTop + (tank.y / WORLD_H) * MINIMAP_SIZE;
            if (tank.team == TEAM_ENEMY) {
                paint.setColor(0xffef4444);
                canvas.drawCircle(sx, sy, 3.8f, paint);
            } else if (tank.id == localPlayerId) {
                paint.setColor(0xffffffff);
                canvas.drawCircle(sx, sy, 3.4f, paint);
            } else {
                paint.setColor(0xcc38bdf8);
                canvas.drawCircle(sx, sy, 2.5f, paint);
            }
        }

        paint.setColor(0xff90be6d);
        float sx = mapLeft + (x / WORLD_W) * MINIMAP_SIZE;
        float sy = mapTop + (y / WORLD_H) * MINIMAP_SIZE;
        canvas.drawCircle(sx, sy, 5.3f, paint);
        drawBaseMinimapMarker(canvas, mapLeft, mapTop, allyBase, 0xff60a5fa);
        drawBaseMinimapMarker(canvas, mapLeft, mapTop, enemyBase, 0xffef4444);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(0xff9aa4b2);
        canvas.drawRoundRect(mapLeft - 2f, mapTop - 2f, mapRight + 2f, mapBottom + 2f, 7f, 7f, paint);
        paint.setColor(0x99828a94);
        canvas.drawRoundRect(mapLeft - 5f, mapTop - 5f, mapRight + 5f, mapBottom + 5f, 8f, 8f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawBaseMinimapMarker(Canvas canvas, float mapLeft, float mapTop, Base base, int color) {
        if (base == null || base.destroyed()) return;
        float sx = mapLeft + (base.x / WORLD_W) * MINIMAP_SIZE;
        float sy = mapTop + (base.y / WORLD_H) * MINIMAP_SIZE;
        paint.setColor(color);
        canvas.drawRect(sx - 4.8f, sy - 4.8f, sx + 4.8f, sy + 4.8f, paint);
    }

    private void drawCenterMessage(Canvas canvas, int width, int height, String text) {
        paint.setTextAlign(Paint.Align.CENTER);
        float maxTextWidth = width * 0.86f;
        float textSize = text.length() > 12 ? 42f : 64f;
        paint.setTextSize(textSize);
        while (paint.measureText(text) > maxTextWidth && textSize > 24f) {
            textSize -= 2f;
            paint.setTextSize(textSize);
        }
        paint.setColor(0xdd000000);
        float boxHalf = Math.min(width * 0.46f, Math.max(180f, paint.measureText(text) * 0.5f + 28f));
        canvas.drawRoundRect(width * 0.5f - boxHalf, height * 0.5f - 70f, width * 0.5f + boxHalf, height * 0.5f + 28f, 12f, 12f, paint);
        paint.setColor(0xffffffff);
        canvas.drawText(text, width * 0.5f, height * 0.5f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private Bitmap tankBodyFor(Tank tank) {
        if (!tank.bot) return tankBlueBodyTexture;
        return tank.team == TEAM_ALLY && allyTankBodyTexture != null ? allyTankBodyTexture : tankWhiteBodyTexture;
    }

    private Bitmap tankTurretFor(Tank tank) {
        if (!tank.bot) return tankBlueTurretTexture;
        return tank.team == TEAM_ALLY && allyTankTurretTexture != null ? allyTankTurretTexture : tankWhiteTurretTexture;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class Base {
        final int team;
        final float x;
        final float y;
        final float size;
        final RectF rect;
        final RectF hitRect;
        final Bitmap texture;
        int hp = BASE_MAX_HP;
        float pendingDamage;

        Base(int team, float x, float y, float size, Bitmap texture) {
            this.team = team;
            this.x = x;
            this.y = y;
            this.size = size;
            this.texture = texture;
            rect = new RectF(x - size * 0.5f, y - size * 0.5f, x + size * 0.5f, y + size * 0.5f);
            hitRect = new RectF(rect);
            hitRect.inset(size * 0.14f, size * 0.14f);
        }

        boolean destroyed() {
            return hp <= 0;
        }

        float repairX() {
            return team == TEAM_ALLY ? x + size * 0.68f : x - size * 0.68f;
        }

        float repairY() {
            return y;
        }

        void drawRepair(Canvas canvas, GameWorld world) {
            if (team != TEAM_ALLY || destroyed()) return;
            float r = size * 0.32f;
            RectF dst = new RectF(repairX() - r, repairY() - r, repairX() + r, repairY() + r);
            if (world.repairZoneTexture != null) {
                canvas.drawBitmap(world.repairZoneTexture, null, dst, world.bitmapPaint);
            } else {
                world.paint.setColor(0x6638bdf8);
                canvas.drawOval(dst, world.paint);
            }
        }

        void draw(Canvas canvas, Paint paint, GameWorld world) {
            Bitmap current = destroyed() && world.destroyedBaseTexture != null
                    ? world.destroyedBaseTexture
                    : (hp < BASE_MAX_HP / 2 && world.damagedBaseTexture != null ? world.damagedBaseTexture : texture);
            if (current != null) {
                canvas.drawBitmap(current, null, rect, world.bitmapPaint);
            } else {
                paint.setColor(team == TEAM_ALLY ? 0xff2563eb : 0xff991b1b);
                canvas.drawRoundRect(rect, 12f, 12f, paint);
            }
            paint.setColor(0xff111827);
            canvas.drawRect(rect.left, rect.top - 18f, rect.right, rect.top - 8f, paint);
            paint.setColor(team == TEAM_ALLY ? 0xff60a5fa : 0xffef4444);
            canvas.drawRect(rect.left + 2f, rect.top - 16f, rect.left + 2f + (rect.width() - 4f) * hp / BASE_MAX_HP, rect.top - 10f, paint);
        }
    }

    private static Bitmap loadTexture(Context context, String path) {
        int maxSize = path.contains("grass") || path.contains("road") ? 256 : 512;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getAssets().open(path)) {
            BitmapFactory.decodeStream(input, null, bounds);
        } catch (IOException ignored) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSize);
        if ("textures/grass.png".equals(path) || "textures/road.png".equals(path)) {
            options.inPreferredConfig = Bitmap.Config.RGB_565;
        }
        try (InputStream input = context.getAssets().open(path)) {
            return BitmapFactory.decodeStream(input, null, options);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static int sampleSize(int width, int height, int maxSize) {
        int sample = 1;
        while (width / (sample * 2) > maxSize || height / (sample * 2) > maxSize) {
            sample *= 2;
        }
        return sample;
    }

    private Bitmap textureFor(int kind) {
        switch (kind) {
            case WALL:
                return wallTexture;
            case BARREL:
                return barrelTexture;
            case FENCE:
                return fenceTexture;
            case BUSH:
                return bushTexture;
            case LAKE:
                return lakeTexture;
            case TREE_1:
                return treeOneTexture;
            case TREE_2:
                return treeTwoTexture;
            case AMMO_CRATE:
                return ammoCrateTexture;
            case CONCRETE_BARRIER:
                return concreteBarrierTexture;
            case CRATER:
                return craterTexture;
            case DESTROYED_TANK:
                return destroyedTankTexture;
            case FUEL_CANISTERS:
                return fuelCanistersTexture;
            case SANDBAGS:
                return sandbagsTexture;
            case WATCHTOWER:
                return watchtowerTexture;
            case TALL_GRASS:
                return tallGrassTexture;
            default:
                return null;
        }
    }

    private static class Obstacle {
        final int kind;
        final RectF rect;
        final RectF hitRect;
        boolean solid;
        boolean blocksShots;
        boolean falling;
        boolean gone;
        float fallTime;
        float fallSign = 1f;

        Obstacle(int kind, float l, float t, float r, float b, boolean solid, boolean blocksShots) {
            this.kind = kind;
            rect = new RectF(l, t, r, b);
            hitRect = makeHitRect(kind, rect);
            this.solid = solid;
            this.blocksShots = blocksShots;
        }

        private static RectF makeHitRect(int kind, RectF rect) {
            RectF hit = new RectF(rect);
            switch (kind) {
                case WALL:
                    hit.inset(rect.width() * 0.13f, rect.height() * 0.12f);
                    break;
                case BARREL:
                    hit.inset(rect.width() * 0.25f, rect.height() * 0.25f);
                    break;
                case FENCE:
                    hit.inset(rect.width() * 0.06f, rect.height() * 0.28f);
                    break;
                case LAKE:
                    hit.inset(rect.width() * 0.18f, rect.height() * 0.20f);
                    break;
                case AMMO_CRATE:
                    hit.inset(rect.width() * 0.22f, rect.height() * 0.24f);
                    break;
                case CONCRETE_BARRIER:
                    hit.inset(rect.width() * 0.08f, rect.height() * 0.28f);
                    break;
                case CRATER:
                    hit.inset(rect.width() * 0.22f, rect.height() * 0.22f);
                    break;
                case DESTROYED_TANK:
                    hit.inset(rect.width() * 0.15f, rect.height() * 0.22f);
                    break;
                case FUEL_CANISTERS:
                    hit.inset(rect.width() * 0.22f, rect.height() * 0.18f);
                    break;
                case SANDBAGS:
                    hit.inset(rect.width() * 0.06f, rect.height() * 0.24f);
                    break;
                case WATCHTOWER:
                    hit.left = rect.left + rect.width() * 0.28f;
                    hit.right = rect.right - rect.width() * 0.28f;
                    hit.top = rect.top + rect.height() * 0.38f;
                    hit.bottom = rect.bottom - rect.height() * 0.08f;
                    break;
                case TREE_1:
                case TREE_2:
                    hit.left = rect.left + rect.width() * 0.40f;
                    hit.right = rect.right - rect.width() * 0.40f;
                    hit.top = rect.top + rect.height() * 0.70f;
                    hit.bottom = rect.bottom - rect.height() * 0.04f;
                    break;
                default:
                    hit.inset(rect.width() * 0.18f, rect.height() * 0.18f);
                    break;
            }
            return hit;
        }

        boolean foreground() {
            return kind == BUSH || kind == TALL_GRASS || isTree();
        }

        boolean isTree() {
            return kind == TREE_1 || kind == TREE_2;
        }

        void draw(Canvas canvas, Paint paint, GameWorld world) {
            Bitmap texture = world.textureFor(kind);
            if (texture != null) {
                world.bitmapPaint.setAlpha(255);
                if (falling && isTree()) {
                    float fall = Math.min(1f, fallTime / TREE_FALL_SECONDS);
                    float eased = 1f - (1f - fall) * (1f - fall);
                    float fade = fallTime <= TREE_FALL_SECONDS ? 1f : 1f - Math.min(1f, (fallTime - TREE_FALL_SECONDS) / (TREE_REMOVE_SECONDS - TREE_FALL_SECONDS));
                    world.bitmapPaint.setAlpha((int) (255f * fade));
                    canvas.save();
                    canvas.rotate(fallSign * 84f * eased, rect.centerX(), rect.bottom - rect.height() * 0.04f);
                    canvas.drawBitmap(texture, null, rect, world.bitmapPaint);
                    canvas.restore();
                    world.bitmapPaint.setAlpha(255);
                    return;
                }
                if (kind == FENCE) {
                    boolean vertical = rect.height() > rect.width() * 1.22f;
                    float drawW = vertical ? Math.min(rect.height(), 220f) : Math.min(rect.width(), 220f);
                    float drawH = vertical ? Math.min(rect.width(), 108f) : Math.min(rect.height(), 108f);
                    world.tempRect.set(rect.centerX() - drawW * 0.5f, rect.centerY() - drawH * 0.5f,
                            rect.centerX() + drawW * 0.5f, rect.centerY() + drawH * 0.5f);
                    if (vertical) {
                        canvas.save();
                        canvas.rotate(90f, rect.centerX(), rect.centerY());
                        canvas.drawBitmap(texture, null, world.tempRect, world.bitmapPaint);
                        canvas.restore();
                    } else {
                        canvas.drawBitmap(texture, null, world.tempRect, world.bitmapPaint);
                    }
                } else if (kind == CRATER) {
                    float side = clamp((rect.width() + rect.height()) * 0.5f, 100f, 154f);
                    world.tempRect.set(rect.centerX() - side * 0.5f, rect.centerY() - side * 0.5f,
                            rect.centerX() + side * 0.5f, rect.centerY() + side * 0.5f);
                    canvas.drawBitmap(texture, null, world.tempRect, world.bitmapPaint);
                } else {
                    canvas.drawBitmap(texture, null, rect, world.bitmapPaint);
                }
                return;
            }
            paint.setStyle(Paint.Style.FILL);
            if (kind == TALL_GRASS) {
                paint.setColor(0xcc4ade3b);
                canvas.drawOval(rect, paint);
                paint.setColor(0x88348122);
                canvas.drawOval(rect.left + rect.width() * 0.12f, rect.top + rect.height() * 0.18f,
                        rect.right - rect.width() * 0.10f, rect.bottom - rect.height() * 0.10f, paint);
            } else {
                paint.setColor(0xff4b5563);
                canvas.drawRoundRect(rect, 8f, 8f, paint);
                paint.setColor(0xff657184);
                canvas.drawRect(rect.left + 8f, rect.top + 8f, rect.right - 8f, rect.top + 18f, paint);
            }
        }
    }

    private static class Scar {
        final float x;
        final float y;
        final float w;
        final float h;
        final float angle;
        final int color;

        Scar(float x, float y, float w, float h, float angle, int color) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.angle = angle;
            this.color = color;
        }
    }

    private static class Crack {
        final float x;
        final float y;
        final float length;
        final float angle;
        final int color;
        final float stroke;
        final boolean branch;

        Crack(float x, float y, float length, float angle, int color, float stroke, boolean branch) {
            this.x = x;
            this.y = y;
            this.length = length;
            this.angle = angle;
            this.color = color;
            this.stroke = stroke;
            this.branch = branch;
        }
    }

    private static class RoadMark {
        static final int TRACKS = 1;
        static final int CRACK = 2;
        static final int DIRT = 3;

        final int kind;
        final float x;
        final float y;
        final float w;
        final float h;
        final float angle;
        final int color;

        RoadMark(int kind, float x, float y, float w, float h, float angle, int color) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.angle = angle;
            this.color = color;
        }
    }

    private static class Tank {
        final int id;
        final boolean bot;
        final int team;
        final int color;
        final String label;
        float x;
        float y;
        float bodyAngle;
        float turretAngle;
        float reload;
        float flash;
        final float aimNoise;
        float avoidSide;
        boolean guard;
        int role = ROLE_ATTACK;
        float respawnTimer;
        boolean reserveHelper;
        boolean helpActive;
        int assignedPlayerId;
        float helpX;
        float helpY;
        float stationX;
        float stationY;
        float lastNavX;
        float lastNavY;
        float stuckTime;
        float detourX;
        float detourY;
        float detourUntil;
        int hp = TANK_MAX_HP;

        Tank(int id, float x, float y, boolean bot, int team, int color, String label) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.bot = bot;
            this.team = team;
            this.color = color;
            this.label = label;
            this.aimNoise = bot ? (float) Math.sin(id * 12.9898f) : 0f;
            this.avoidSide = id % 2 == 0 ? 1f : -1f;
        }

        void draw(Canvas canvas, Paint paint, GameWorld world, boolean selected) {
            Bitmap body = world.tankBodyFor(this);
            Bitmap turret = world.tankTurretFor(this);
            if (body != null && turret != null) {
                if (selected) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(4f);
                    paint.setColor(0xccffffff);
                    canvas.drawCircle(x, y, 44f, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                drawBitmapCentered(canvas, world.bitmapPaint, body, x, y, 86f, 86f, bodyAngle + (float) Math.PI * 0.5f);
                drawBitmapCentered(canvas, world.bitmapPaint, turret, x, y - 2f, 92f, 92f, turretAngle + (float) Math.PI * 0.5f);
                if (flash > 0f && world.muzzleFlashTexture != null) {
                    float fx = x + (float) Math.cos(turretAngle) * 48f;
                    float fy = y + (float) Math.sin(turretAngle) * 48f;
                    world.bitmapPaint.setAlpha((int) (255f * Math.min(1f, flash / 0.09f)));
                    drawBitmapCentered(canvas, world.bitmapPaint, world.muzzleFlashTexture, fx, fy, 42f, 42f, turretAngle + (float) Math.PI * 0.5f);
                    world.bitmapPaint.setAlpha(255);
                }
            } else {
                canvas.save();
                canvas.translate(x, y);
                canvas.rotate((float) Math.toDegrees(bodyAngle));
                paint.setColor(selected ? 0xffffffff : 0xff111827);
                canvas.drawCircle(0, 0, 36f, paint);
                paint.setColor(color);
                canvas.drawRoundRect(-30f, -22f, 30f, 22f, 8f, 8f, paint);
                canvas.restore();

                canvas.save();
                canvas.translate(x, y);
                canvas.rotate((float) Math.toDegrees(turretAngle));
                paint.setColor(0xffe5e7eb);
                canvas.drawRoundRect(-8f, -8f, 48f, 8f, 7f, 7f, paint);
                canvas.restore();
            }

            paint.setColor(0xff111827);
            canvas.drawRect(x - 31f, y - 50f, x + 31f, y - 43f, paint);
            paint.setColor(hp > 55 ? 0xff90be6d : 0xfff94144);
            canvas.drawRect(x - 30f, y - 49f, x - 30f + 60f * hp / TANK_MAX_HP, y - 44f, paint);
            paint.setTextSize(18f);
            paint.setColor(0xffffffff);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(label, x, y + 58f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        private void drawBitmapCentered(Canvas canvas, Paint paint, Bitmap bitmap, float cx, float cy, float w, float h, float angle) {
            canvas.save();
            canvas.translate(cx, cy);
            canvas.rotate((float) Math.toDegrees(angle));
            RectF dst = new RectF(-w * 0.5f, -h * 0.5f, w * 0.5f, h * 0.5f);
            canvas.drawBitmap(bitmap, null, dst, paint);
            canvas.restore();
        }
    }

    private static class Bullet {
        final float vx;
        final float vy;
        final int owner;
        final int team;
        final float angle;
        float x;
        float y;
        float life = 1.7f;

        Bullet(float x, float y, float vx, float vy, int owner, int team, float angle) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.owner = owner;
            this.team = team;
            this.angle = angle;
        }

        void draw(Canvas canvas, Paint paint, GameWorld world) {
            if (world.bulletTexture != null) {
                canvas.save();
                canvas.translate(x, y);
                canvas.rotate((float) Math.toDegrees(angle + (float) Math.PI * 0.5f));
                RectF dst = new RectF(-17f, -31f, 17f, 31f);
                canvas.drawBitmap(world.bulletTexture, null, dst, world.bitmapPaint);
                canvas.restore();
            } else {
                paint.setColor(0xfffff3b0);
                canvas.drawCircle(x, y, 12f, paint);
            }
        }
    }
}
