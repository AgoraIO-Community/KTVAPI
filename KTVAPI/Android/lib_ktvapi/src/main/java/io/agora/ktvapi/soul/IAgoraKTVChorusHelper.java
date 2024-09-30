package io.agora.ktvapi.soul;

public interface IAgoraKTVChorusHelper {
    void playKTVEncryptAudio(PlayKTVParams params, Boolean isAccompanyDelayPositionChange);

    void stopAudio();

    void setNewLeadSinger(int uid);
}
