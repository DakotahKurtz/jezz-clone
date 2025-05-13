package edu.bloomu.animationtest;

/**
 * Enum to allow for clearer code when control flow needs to be determined by the
 * current state of the application.
 */
public enum GameState {
    PAUSED,
    GAME_OVER,
    LEVEL_WON,
    MENU_SCREEN,
    GAME_ACTIVE,
    SPHERE_SCREEN,
    GALLERY_SCREEN
}
