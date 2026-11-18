package MeongDev.DIRUNA.GloftDIRU.core.input;

/**
 * State-aware, platform-independent model of the source game's in-stage input
 * gates. It returns actions for the game layer; it does not execute gameplay,
 * mutate player coordinates, or call Android APIs.
 */
public final class LegacyInputDispatcher {
    public enum Kind {
        BLOCKED, NO_INPUT, NORMAL_DIRECTION, CENTER_ACTION,
        INTERACTION_CENTER, INTERACTION_CONFIRM, INTERACTION_IDLE,
        SPECIAL_MOVEMENT, SPECIAL_CENTER_CONSUMED,
        SPECIAL_CONFIRM_ACTION, SPECIAL_WAIT
    }

    public static final class Context {
        private final boolean clearInputBeforeDispatch;
        private final int clInt;
        private final int ayInt;
        private final boolean scriptedExit;
        private final int playerStatus;
        private final boolean deathAnimation;
        private final boolean specialActionFlag;
        private final boolean interactionPresent;
        private final int specialMovementTicks;

        public Context(boolean clearInputBeforeDispatch, int clInt, int ayInt,
                       boolean scriptedExit, int playerStatus,
                       boolean deathAnimation, boolean specialActionFlag,
                       boolean interactionPresent, int specialMovementTicks) {
            this.clearInputBeforeDispatch = clearInputBeforeDispatch;
            this.clInt = clInt;
            this.ayInt = ayInt;
            this.scriptedExit = scriptedExit;
            this.playerStatus = playerStatus;
            this.deathAnimation = deathAnimation;
            this.specialActionFlag = specialActionFlag;
            this.interactionPresent = interactionPresent;
            this.specialMovementTicks = specialMovementTicks;
        }
    }

    public static final class Result {
        private final Kind kind;
        private final int direction;
        private final int offsetX;
        private final int offsetY;
        private final boolean consumesSpecialMovementTick;

        private Result(Kind kind, int direction, int offsetX, int offsetY,
                       boolean consumesSpecialMovementTick) {
            this.kind = kind;
            this.direction = direction;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.consumesSpecialMovementTick = consumesSpecialMovementTick;
        }

        public Kind kind() { return kind; }
        public int direction() { return direction; }
        public int offsetX() { return offsetX; }
        public int offsetY() { return offsetY; }
        public boolean consumesSpecialMovementTick() {
            return consumesSpecialMovementTick;
        }
    }

    private LegacyInputDispatcher() {
    }

    public static Result dispatch(HeldKeyState keys, Context context) {
        if (context.clearInputBeforeDispatch) {
            keys.clear();
        }

        boolean confirmPressed = keys.isAnyPressed(LegacyInputMasks.CONFIRM);
        boolean movementLocked =
                (context.clInt != 0 && !confirmPressed)
                || context.ayInt != 0
                || context.scriptedExit
                || context.playerStatus <= 0
                || context.deathAnimation
                || context.specialActionFlag;

        if (movementLocked) {
            keys.clear();
            return result(Kind.BLOCKED);
        }

        if (context.interactionPresent) {
            Kind kind;
            if (keys.isAnyPressed(LegacyInputMasks.CENTER)) {
                kind = Kind.INTERACTION_CENTER;
            } else if (confirmPressed) {
                kind = Kind.INTERACTION_CONFIRM;
            } else {
                kind = Kind.INTERACTION_IDLE;
            }
            keys.clear();
            return result(kind);
        }

        if (context.specialMovementTicks > 0) {
            int direction = LegacyDirectionResolver.resolveNormalStageDirection(keys);
            if (direction != LegacyDirectionResolver.NONE) {
                int offsetX = 0;
                int offsetY = 0;
                if (direction == LegacyDirectionResolver.UP) {
                    offsetY = -5;
                } else if (direction == LegacyDirectionResolver.DOWN) {
                    offsetY = 5;
                } else if (direction == LegacyDirectionResolver.LEFT) {
                    offsetX = -5;
                } else if (direction == LegacyDirectionResolver.RIGHT) {
                    offsetX = 5;
                }
                keys.clear();
                return new Result(Kind.SPECIAL_MOVEMENT, direction,
                        offsetX, offsetY, true);
            }
            if (keys.isAnyPressed(LegacyInputMasks.CENTER)) {
                keys.clear();
                return new Result(Kind.SPECIAL_CENTER_CONSUMED,
                        LegacyDirectionResolver.NONE, 0, 0, true);
            }
            if (confirmPressed) {
                keys.clear();
                return new Result(Kind.SPECIAL_CONFIRM_ACTION,
                        LegacyDirectionResolver.NONE, 0, 0, true);
            }
            return result(Kind.SPECIAL_WAIT);
        }

        int direction = LegacyDirectionResolver.resolveNormalStageDirection(keys);
        if (direction != LegacyDirectionResolver.NONE) {
            return new Result(Kind.NORMAL_DIRECTION, direction, 0, 0, false);
        }
        if (keys.isAnyPressed(LegacyInputMasks.CENTER)) {
            keys.clear();
            return result(Kind.CENTER_ACTION);
        }
        return result(Kind.NO_INPUT);
    }

    private static Result result(Kind kind) {
        return new Result(kind, LegacyDirectionResolver.NONE, 0, 0, false);
    }
}
