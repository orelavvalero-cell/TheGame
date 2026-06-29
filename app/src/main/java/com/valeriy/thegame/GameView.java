package com.valeriy.thegame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    interface StatusSink {
        void setStatus(String text);
    }

    private final GameWorld world;
    private final InputState localInput = new InputState();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final NetworkGame network;
    private Thread thread;
    private volatile boolean running;
    private StatusSink statusSink;

    private static final float DEAD_ZONE = 0.17f;
    private static final long TARGET_FRAME_MS = 33L;
    private int movePointer = -1;
    private int firePointer = -1;
    private float moveBaseX;
    private float moveBaseY;
    private float moveX;
    private float moveY;
    private float fireX;
    private float fireY;
    private float stickRadius;
    private float knobRadius;
    private float fireRadius;
    private int lastControlW;
    private int lastControlH;
    private boolean shooting;
    private int helpFrames;
    private int stopHelpFrames;
    private boolean helpActiveUi;
    private boolean currentAuthoritative = true;
    private int currentLocalPlayerId = 1;
    private boolean currentMultiplayer;

    GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        world = new GameWorld(context);
        network = new NetworkGame(context, world, localInput, this::postStatus);
    }

    void setStatusSink(StatusSink sink) {
        statusSink = sink;
    }

    void startSolo() {
        network.stop();
        currentAuthoritative = true;
        currentLocalPlayerId = 1;
        currentMultiplayer = false;
        world.reset(true, 1);
        postStatus("SOLO: bots active");
    }

    void startHost() {
        currentAuthoritative = true;
        currentLocalPlayerId = 1;
        currentMultiplayer = true;
        world.reset(true, 1, true);
        network.startHost();
    }

    void findAndJoinHost() {
        currentAuthoritative = false;
        currentLocalPlayerId = 2;
        currentMultiplayer = true;
        world.reset(false, 2, true);
        network.startClient();
    }

    void restartRound() {
        world.reset(currentAuthoritative, currentLocalPlayerId, currentMultiplayer);
        postStatus(currentMultiplayer ? "Round restarted" : "SOLO restarted");
    }

    boolean toggleHelp() {
        if (helpActiveUi) {
            stopHelpFrames = 18;
            localInput.setStopHelpRequested(true);
            helpActiveUi = false;
            postStatus("STOP HELP: reserve returning");
        } else {
            helpFrames = 18;
            localInput.setHelpRequested(true);
            helpActiveUi = true;
            postStatus("GET HELP: reserve moving");
        }
        return helpActiveUi;
    }

    void stopNetwork() {
        network.stop();
        currentAuthoritative = true;
        currentLocalPlayerId = 1;
        currentMultiplayer = false;
        world.reset(true, 1);
        postStatus("Network stopped. SOLO active");
    }

    void shutdown() {
        network.stop();
        pause();
    }

    void resume() {
        if (running) return;
        running = true;
        thread = new Thread(this, "TankGameLoop");
        thread.start();
    }

    void pause() {
        running = false;
        Thread t = thread;
        thread = null;
        if (t != null) {
            try {
                t.join(800);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = Math.min(0.033f, (now - last) / 1_000_000_000f);
            last = now;
            localInput.setHelpRequested(helpFrames > 0);
            localInput.setStopHelpRequested(stopHelpFrames > 0);
            world.setInput(world.getLocalPlayerId(), localInput.copy());
            if (helpFrames > 0) helpFrames--;
            if (stopHelpFrames > 0) stopHelpFrames--;
            world.update(dt);
            drawFrame();
            long frameMs = (System.nanoTime() - now) / 1_000_000L;
            if (frameMs < TARGET_FRAME_MS) {
                try {
                    Thread.sleep(TARGET_FRAME_MS - frameMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void drawFrame() {
        SurfaceHolder holder = getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;
        try {
            world.draw(canvas, getWidth(), getHeight());
            drawControls(canvas);
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawControls(Canvas canvas) {
        ensureControls();
        drawPad(canvas, moveBaseX, moveBaseY, moveX, moveY, 0xff69d2e7);
        drawFireButton(canvas);
    }

    private void drawPad(Canvas canvas, float bx, float by, float x, float y, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x44000000);
        canvas.drawCircle(bx, by, stickRadius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(0x88ffffff);
        canvas.drawCircle(bx, by, stickRadius, paint);
        paint.setColor(0x33ffffff);
        canvas.drawCircle(bx, by, stickRadius * DEAD_ZONE, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawCircle(x, y, knobRadius, paint);
        paint.setColor(0x55000000);
        canvas.drawCircle(x - knobRadius * 0.22f, y - knobRadius * 0.22f, knobRadius * 0.36f, paint);
    }

    private void drawFireButton(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(shooting ? 0xddf59e0b : 0x88f59e0b);
        canvas.drawCircle(fireX, fireY, fireRadius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(0xddffffff);
        canvas.drawCircle(fireX, fireY, fireRadius, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xffffffff);
        float shellW = fireRadius * 0.22f;
        float shellH = fireRadius * 0.62f;
        canvas.save();
        canvas.rotate(-28f, fireX, fireY);
        canvas.drawRoundRect(fireX - shellW, fireY - shellH * 0.5f, fireX + shellW, fireY + shellH * 0.5f, shellW, shellW, paint);
        canvas.drawCircle(fireX, fireY - shellH * 0.5f, shellW, paint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        ensureControls();
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int pointerId = event.getPointerId(index);
            float x = event.getX(index);
            float y = event.getY(index);
            if (isInside(x, y, fireX, fireY, fireRadius * 1.25f) && firePointer == -1) {
                firePointer = pointerId;
                shooting = true;
            } else if (x < getWidth() * 0.5f && movePointer == -1) {
                movePointer = pointerId;
                moveX = x;
                moveY = y;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            int pointerId = event.getPointerId(index);
            if (pointerId == movePointer || action == MotionEvent.ACTION_CANCEL) {
                movePointer = -1;
                moveX = moveBaseX;
                moveY = moveBaseY;
            }
            if (pointerId == firePointer || action == MotionEvent.ACTION_CANCEL) {
                firePointer = -1;
                shooting = false;
            }
        }

        for (int i = 0; i < event.getPointerCount(); i++) {
            int pointerId = event.getPointerId(i);
            if (pointerId == movePointer) {
                moveX = event.getX(i);
                moveY = event.getY(i);
            }
        }
        updateInputFromPads();
        return true;
    }

    private void updateInputFromPads() {
        float rawMoveX = (moveX - moveBaseX) / stickRadius;
        float rawMoveY = (moveY - moveBaseY) / stickRadius;
        float moveLen = (float) Math.hypot(rawMoveX, rawMoveY);
        if (moveLen > 1f) {
            rawMoveX /= moveLen;
            rawMoveY /= moveLen;
            moveX = moveBaseX + rawMoveX * stickRadius;
            moveY = moveBaseY + rawMoveY * stickRadius;
            moveLen = 1f;
        }
        float outputMoveX = applyDeadZone(rawMoveX, moveLen);
        float outputMoveY = applyDeadZone(rawMoveY, moveLen);
        float aimX = localInput.aimX;
        float aimY = localInput.aimY;
        if (moveLen >= DEAD_ZONE) {
            aimX = rawMoveX / Math.max(0.001f, moveLen);
            aimY = rawMoveY / Math.max(0.001f, moveLen);
        }
        localInput.set(outputMoveX, outputMoveY, aimX, aimY, shooting);
    }

    private float applyDeadZone(float value, float len) {
        if (len < DEAD_ZONE) return 0f;
        float scaled = (len - DEAD_ZONE) / (1f - DEAD_ZONE);
        scaled = Math.min(1f, scaled);
        return value / Math.max(0.001f, len) * scaled;
    }

    private void ensureControls() {
        int w = getWidth();
        int h = getHeight();
        if (w == lastControlW && h == lastControlH && stickRadius > 0f) return;
        lastControlW = w;
        lastControlH = h;
        stickRadius = Math.max(dp(64), Math.min(dp(88), h * 0.19f));
        knobRadius = Math.max(dp(25), stickRadius * 0.38f);
        fireRadius = Math.max(dp(38), Math.min(dp(52), h * 0.12f));
        moveBaseX = dp(118);
        moveBaseY = h - dp(108);
        fireX = w - dp(74);
        fireY = h - dp(128);
        if (fireY < dp(92)) fireY = dp(92);
        moveX = moveBaseX;
        moveY = moveBaseY;
    }

    private boolean isInside(float x, float y, float cx, float cy, float radius) {
        float dx = x - cx;
        float dy = y - cy;
        return dx * dx + dy * dy <= radius * radius;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void postStatus(String text) {
        if (statusSink != null) statusSink.setStatus(text);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }
}
