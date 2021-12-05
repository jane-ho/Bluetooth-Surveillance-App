package com.makerlab.example.protocol;

/*
https://vorpalrobotics.com/wiki/index.php/Vorpal_The_Hexapod_Radio_Protocol_Technical_Information
 */
public class Vorpal {
    final int LEN_SLOT = 2;
    final int DATA_SLOT = 3;
    final int CHECKSUM_SLOT = 6;
    //
    public Vorpal() {
    }

    public byte[] goForward() {
        WalkPatterns walkPatterns = WalkPatterns.FORWARD;
        return getPayload(walkPatterns);
    }

    public byte[] goBackward() {
        WalkPatterns walkPatterns = WalkPatterns.BACKWARD;
        return getPayload(walkPatterns);
    }

    public byte[] goLeft() {
        WalkPatterns walkPatterns = WalkPatterns.LEFT;
        return getPayload(walkPatterns);
    }

    public byte[] goRight() {
        WalkPatterns walkPatterns = WalkPatterns.RIGHT;
        return getPayload(walkPatterns);
    }

    public byte[] stomp() {
        WalkPatterns walkPatterns = WalkPatterns.STOMP;
        return getPayload(walkPatterns);
    }

    public byte[] stop() {
        WalkPatterns walkPatterns = WalkPatterns.STOP;
        return getPayload(walkPatterns);
    }

    public byte[] getPayload(WalkPatterns walkPatterns) {
        final MovementModes movement = MovementModes.WALK;
        //final WalkModes walkMode = WalkModes.HIGH_STEP;
        final WalkModes walkMode = WalkModes.SMALL_STEP;
        byte[] payload = {'V', '1', 0, 0, 0, 0, 0};

        payload[LEN_SLOT] = 3;
        payload[DATA_SLOT] = movement.getValue();
        payload[DATA_SLOT + 1] = walkMode.getValue();
        payload[DATA_SLOT + 2] = walkPatterns.getValue();
        payload[CHECKSUM_SLOT] = calCheckSum(payload);
        return payload;

    }

    private byte calCheckSum(byte[] payload) {
        int sum = 0;
        int len = payload[LEN_SLOT];
        for (int i = LEN_SLOT; i < DATA_SLOT + len; i++) {
            sum += payload[i];
        }
        sum = sum % 256;
        return (byte) sum;
    }

    enum MovementModes {
        WALK,
        DANCE,
        FIGHT;

        public byte getValue() {
            switch (this) {
                case WALK:
                    return 'W';
                case DANCE:
                    return 'D';
                case FIGHT:
                    return 'F';
                default:
                    throw new AssertionError("unknown movement mode " + this);
            }
        }
    }

    enum WalkModes {
        LOW_STEP,
        HIGH_STEP,
        SMALL_STEP,
        SCAMPER;

        public byte getValue() {
            switch (this) {
                case LOW_STEP:
                    return '1';
                case HIGH_STEP:
                    return '2';
                case SMALL_STEP:
                    return '3';
                case SCAMPER:
                    return '4';
                default:
                    throw new AssertionError("Unknown walk mode " + this);
            }
        }
    }

    public enum WalkPatterns {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        STOMP,
        STOP;

        public byte getValue() {
            switch (this) {
                case FORWARD:
                    return 'f';
                case BACKWARD:
                    return 'b';
                case LEFT:
                    return 'l';
                case RIGHT:
                    return 'r';
                case STOMP:
                    return 'w';
                case STOP:
                    return 's';
                default:
                    throw new AssertionError("Unknown direction " + this);
            }
        }
    }
    //
    enum DanceModes {
        FREESTYLE,
        BALLET,
        WAVE,
        HANDS;
        public byte getValue() {
            switch (this) {
                case FREESTYLE:
                    return '1';
                case BALLET:
                    return '2';
                case WAVE:
                    return '3';
                case HANDS:
                    return '4';
                default:
                    throw new AssertionError("Unknown walk mode " + this);
            }
        }
    }

    public enum  DancePattern {

    }

    //
    enum FightModes {
        FL_FIGHT,
        FL_UNISON_FIGHT,
        SWIVEL_THRUST,
        LEAN;
        public byte getValue() {
            switch (this) {
                case FL_FIGHT:
                    return '1';
                case FL_UNISON_FIGHT:
                    return '2';
                case SWIVEL_THRUST:
                    return '3';
                case LEAN:
                    return '4';
                default:
                    throw new AssertionError("Unknown walk mode " + this);
            }
        }
    }
    public enum FightPattern {

    }
}
