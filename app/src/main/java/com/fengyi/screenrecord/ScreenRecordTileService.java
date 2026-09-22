package com.fengyi.screenrecord;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * 控制中心磁贴：点一下启动 screenrecord，再点一下停止。
 * 状态直接读取系统里 screenrecord 进程是否存在。
 */
public class ScreenRecordTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        RecordManager manager = RecordManager.get(this);
        if (manager.isRecording()) {
            manager.stopRecording();
        } else {
            manager.startRecording();
        }
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean rec = RecordManager.get(this).isRecording();
        tile.setState(rec ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(rec ? getString(R.string.btn_stop) : getString(R.string.btn_record));
        tile.updateTile();
    }
}
