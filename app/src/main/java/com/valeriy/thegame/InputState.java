package com.valeriy.thegame;

class InputState {
    float moveX;
    float moveY;
    float aimX = 1f;
    float aimY;
    boolean shooting;
    boolean helpRequested;
    boolean stopHelpRequested;

    synchronized void set(float moveX, float moveY, float aimX, float aimY, boolean shooting) {
        this.moveX = clamp(moveX, -1f, 1f);
        this.moveY = clamp(moveY, -1f, 1f);
        float len = (float) Math.hypot(aimX, aimY);
        if (len > 0.08f) {
            this.aimX = aimX / len;
            this.aimY = aimY / len;
        }
        this.shooting = shooting;
    }

    synchronized InputState copy() {
        InputState copy = new InputState();
        copy.moveX = moveX;
        copy.moveY = moveY;
        copy.aimX = aimX;
        copy.aimY = aimY;
        copy.shooting = shooting;
        copy.helpRequested = helpRequested;
        copy.stopHelpRequested = stopHelpRequested;
        return copy;
    }

    String toWire() {
        InputState c = copy();
        return "IN " + c.moveX + " " + c.moveY + " " + c.aimX + " " + c.aimY + " "
                + (c.shooting ? 1 : 0) + " " + (c.helpRequested ? 1 : 0) + " " + (c.stopHelpRequested ? 1 : 0);
    }

    synchronized void setHelpRequested(boolean helpRequested) {
        this.helpRequested = helpRequested;
    }

    synchronized void setStopHelpRequested(boolean stopHelpRequested) {
        this.stopHelpRequested = stopHelpRequested;
    }

    static InputState fromWire(String line) {
        String[] p = line.split(" ");
        if (p.length < 6 || !"IN".equals(p[0])) return null;
        InputState input = new InputState();
        try {
            input.set(Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3]), Float.parseFloat(p[4]), "1".equals(p[5]));
            input.setHelpRequested(p.length > 6 && "1".equals(p[6]));
            input.setStopHelpRequested(p.length > 7 && "1".equals(p[7]));
            return input;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
