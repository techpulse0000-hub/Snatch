package com.ibnluqman.vidgrab;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DownloadDao_Impl implements DownloadDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DownloadRecord> __insertionAdapterOfDownloadRecord;

  private final EntityDeletionOrUpdateAdapter<DownloadRecord> __deletionAdapterOfDownloadRecord;

  private final EntityDeletionOrUpdateAdapter<DownloadRecord> __updateAdapterOfDownloadRecord;

  public DownloadDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDownloadRecord = new EntityInsertionAdapter<DownloadRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `downloads` (`id`,`url`,`forcedQuality`,`mediaType`,`title`,`status`,`progress`,`speedText`,`etaText`,`filePath`,`thumbnailUrl`,`errorMessage`,`createdAt`,`downloadedBytes`,`totalBytes`,`playlistIndex`,`carouselGroupId`,`carouselIndex`,`carouselTotal`,`isDirectMediaUrl`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getUrl());
        if (entity.getForcedQuality() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getForcedQuality());
        }
        if (entity.getMediaType() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getMediaType());
        }
        statement.bindString(5, entity.getTitle());
        statement.bindString(6, entity.getStatus());
        statement.bindLong(7, entity.getProgress());
        statement.bindString(8, entity.getSpeedText());
        statement.bindString(9, entity.getEtaText());
        if (entity.getFilePath() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getFilePath());
        }
        if (entity.getThumbnailUrl() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getThumbnailUrl());
        }
        if (entity.getErrorMessage() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getErrorMessage());
        }
        statement.bindLong(13, entity.getCreatedAt());
        statement.bindLong(14, entity.getDownloadedBytes());
        statement.bindLong(15, entity.getTotalBytes());
        if (entity.getPlaylistIndex() == null) {
          statement.bindNull(16);
        } else {
          statement.bindLong(16, entity.getPlaylistIndex());
        }
        if (entity.getCarouselGroupId() == null) {
          statement.bindNull(17);
        } else {
          statement.bindLong(17, entity.getCarouselGroupId());
        }
        if (entity.getCarouselIndex() == null) {
          statement.bindNull(18);
        } else {
          statement.bindLong(18, entity.getCarouselIndex());
        }
        if (entity.getCarouselTotal() == null) {
          statement.bindNull(19);
        } else {
          statement.bindLong(19, entity.getCarouselTotal());
        }
        final int _tmp = entity.isDirectMediaUrl() ? 1 : 0;
        statement.bindLong(20, _tmp);
      }
    };
    this.__deletionAdapterOfDownloadRecord = new EntityDeletionOrUpdateAdapter<DownloadRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `downloads` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadRecord entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfDownloadRecord = new EntityDeletionOrUpdateAdapter<DownloadRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `downloads` SET `id` = ?,`url` = ?,`forcedQuality` = ?,`mediaType` = ?,`title` = ?,`status` = ?,`progress` = ?,`speedText` = ?,`etaText` = ?,`filePath` = ?,`thumbnailUrl` = ?,`errorMessage` = ?,`createdAt` = ?,`downloadedBytes` = ?,`totalBytes` = ?,`playlistIndex` = ?,`carouselGroupId` = ?,`carouselIndex` = ?,`carouselTotal` = ?,`isDirectMediaUrl` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getUrl());
        if (entity.getForcedQuality() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getForcedQuality());
        }
        if (entity.getMediaType() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getMediaType());
        }
        statement.bindString(5, entity.getTitle());
        statement.bindString(6, entity.getStatus());
        statement.bindLong(7, entity.getProgress());
        statement.bindString(8, entity.getSpeedText());
        statement.bindString(9, entity.getEtaText());
        if (entity.getFilePath() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getFilePath());
        }
        if (entity.getThumbnailUrl() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getThumbnailUrl());
        }
        if (entity.getErrorMessage() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getErrorMessage());
        }
        statement.bindLong(13, entity.getCreatedAt());
        statement.bindLong(14, entity.getDownloadedBytes());
        statement.bindLong(15, entity.getTotalBytes());
        if (entity.getPlaylistIndex() == null) {
          statement.bindNull(16);
        } else {
          statement.bindLong(16, entity.getPlaylistIndex());
        }
        if (entity.getCarouselGroupId() == null) {
          statement.bindNull(17);
        } else {
          statement.bindLong(17, entity.getCarouselGroupId());
        }
        if (entity.getCarouselIndex() == null) {
          statement.bindNull(18);
        } else {
          statement.bindLong(18, entity.getCarouselIndex());
        }
        if (entity.getCarouselTotal() == null) {
          statement.bindNull(19);
        } else {
          statement.bindLong(19, entity.getCarouselTotal());
        }
        final int _tmp = entity.isDirectMediaUrl() ? 1 : 0;
        statement.bindLong(20, _tmp);
        statement.bindLong(21, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final DownloadRecord record, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfDownloadRecord.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final DownloadRecord record, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDownloadRecord.handle(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final DownloadRecord record, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDownloadRecord.handle(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DownloadRecord>> observeAll() {
    final String _sql = "SELECT * FROM downloads ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"downloads"}, new Callable<List<DownloadRecord>>() {
      @Override
      @NonNull
      public List<DownloadRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfForcedQuality = CursorUtil.getColumnIndexOrThrow(_cursor, "forcedQuality");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfSpeedText = CursorUtil.getColumnIndexOrThrow(_cursor, "speedText");
          final int _cursorIndexOfEtaText = CursorUtil.getColumnIndexOrThrow(_cursor, "etaText");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfDownloadedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedBytes");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfPlaylistIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistIndex");
          final int _cursorIndexOfCarouselGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "carouselGroupId");
          final int _cursorIndexOfCarouselIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "carouselIndex");
          final int _cursorIndexOfCarouselTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "carouselTotal");
          final int _cursorIndexOfIsDirectMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirectMediaUrl");
          final List<DownloadRecord> _result = new ArrayList<DownloadRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DownloadRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpForcedQuality;
            if (_cursor.isNull(_cursorIndexOfForcedQuality)) {
              _tmpForcedQuality = null;
            } else {
              _tmpForcedQuality = _cursor.getString(_cursorIndexOfForcedQuality);
            }
            final String _tmpMediaType;
            if (_cursor.isNull(_cursorIndexOfMediaType)) {
              _tmpMediaType = null;
            } else {
              _tmpMediaType = _cursor.getString(_cursorIndexOfMediaType);
            }
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final int _tmpProgress;
            _tmpProgress = _cursor.getInt(_cursorIndexOfProgress);
            final String _tmpSpeedText;
            _tmpSpeedText = _cursor.getString(_cursorIndexOfSpeedText);
            final String _tmpEtaText;
            _tmpEtaText = _cursor.getString(_cursorIndexOfEtaText);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpDownloadedBytes;
            _tmpDownloadedBytes = _cursor.getLong(_cursorIndexOfDownloadedBytes);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final Integer _tmpPlaylistIndex;
            if (_cursor.isNull(_cursorIndexOfPlaylistIndex)) {
              _tmpPlaylistIndex = null;
            } else {
              _tmpPlaylistIndex = _cursor.getInt(_cursorIndexOfPlaylistIndex);
            }
            final Long _tmpCarouselGroupId;
            if (_cursor.isNull(_cursorIndexOfCarouselGroupId)) {
              _tmpCarouselGroupId = null;
            } else {
              _tmpCarouselGroupId = _cursor.getLong(_cursorIndexOfCarouselGroupId);
            }
            final Integer _tmpCarouselIndex;
            if (_cursor.isNull(_cursorIndexOfCarouselIndex)) {
              _tmpCarouselIndex = null;
            } else {
              _tmpCarouselIndex = _cursor.getInt(_cursorIndexOfCarouselIndex);
            }
            final Integer _tmpCarouselTotal;
            if (_cursor.isNull(_cursorIndexOfCarouselTotal)) {
              _tmpCarouselTotal = null;
            } else {
              _tmpCarouselTotal = _cursor.getInt(_cursorIndexOfCarouselTotal);
            }
            final boolean _tmpIsDirectMediaUrl;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirectMediaUrl);
            _tmpIsDirectMediaUrl = _tmp != 0;
            _item = new DownloadRecord(_tmpId,_tmpUrl,_tmpForcedQuality,_tmpMediaType,_tmpTitle,_tmpStatus,_tmpProgress,_tmpSpeedText,_tmpEtaText,_tmpFilePath,_tmpThumbnailUrl,_tmpErrorMessage,_tmpCreatedAt,_tmpDownloadedBytes,_tmpTotalBytes,_tmpPlaylistIndex,_tmpCarouselGroupId,_tmpCarouselIndex,_tmpCarouselTotal,_tmpIsDirectMediaUrl);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final long id, final Continuation<? super DownloadRecord> $completion) {
    final String _sql = "SELECT * FROM downloads WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DownloadRecord>() {
      @Override
      @Nullable
      public DownloadRecord call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfForcedQuality = CursorUtil.getColumnIndexOrThrow(_cursor, "forcedQuality");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfSpeedText = CursorUtil.getColumnIndexOrThrow(_cursor, "speedText");
          final int _cursorIndexOfEtaText = CursorUtil.getColumnIndexOrThrow(_cursor, "etaText");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfDownloadedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedBytes");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfPlaylistIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistIndex");
          final int _cursorIndexOfCarouselGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "carouselGroupId");
          final int _cursorIndexOfCarouselIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "carouselIndex");
          final int _cursorIndexOfCarouselTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "carouselTotal");
          final int _cursorIndexOfIsDirectMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirectMediaUrl");
          final DownloadRecord _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpForcedQuality;
            if (_cursor.isNull(_cursorIndexOfForcedQuality)) {
              _tmpForcedQuality = null;
            } else {
              _tmpForcedQuality = _cursor.getString(_cursorIndexOfForcedQuality);
            }
            final String _tmpMediaType;
            if (_cursor.isNull(_cursorIndexOfMediaType)) {
              _tmpMediaType = null;
            } else {
              _tmpMediaType = _cursor.getString(_cursorIndexOfMediaType);
            }
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final int _tmpProgress;
            _tmpProgress = _cursor.getInt(_cursorIndexOfProgress);
            final String _tmpSpeedText;
            _tmpSpeedText = _cursor.getString(_cursorIndexOfSpeedText);
            final String _tmpEtaText;
            _tmpEtaText = _cursor.getString(_cursorIndexOfEtaText);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpDownloadedBytes;
            _tmpDownloadedBytes = _cursor.getLong(_cursorIndexOfDownloadedBytes);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final Integer _tmpPlaylistIndex;
            if (_cursor.isNull(_cursorIndexOfPlaylistIndex)) {
              _tmpPlaylistIndex = null;
            } else {
              _tmpPlaylistIndex = _cursor.getInt(_cursorIndexOfPlaylistIndex);
            }
            final Long _tmpCarouselGroupId;
            if (_cursor.isNull(_cursorIndexOfCarouselGroupId)) {
              _tmpCarouselGroupId = null;
            } else {
              _tmpCarouselGroupId = _cursor.getLong(_cursorIndexOfCarouselGroupId);
            }
            final Integer _tmpCarouselIndex;
            if (_cursor.isNull(_cursorIndexOfCarouselIndex)) {
              _tmpCarouselIndex = null;
            } else {
              _tmpCarouselIndex = _cursor.getInt(_cursorIndexOfCarouselIndex);
            }
            final Integer _tmpCarouselTotal;
            if (_cursor.isNull(_cursorIndexOfCarouselTotal)) {
              _tmpCarouselTotal = null;
            } else {
              _tmpCarouselTotal = _cursor.getInt(_cursorIndexOfCarouselTotal);
            }
            final boolean _tmpIsDirectMediaUrl;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirectMediaUrl);
            _tmpIsDirectMediaUrl = _tmp != 0;
            _result = new DownloadRecord(_tmpId,_tmpUrl,_tmpForcedQuality,_tmpMediaType,_tmpTitle,_tmpStatus,_tmpProgress,_tmpSpeedText,_tmpEtaText,_tmpFilePath,_tmpThumbnailUrl,_tmpErrorMessage,_tmpCreatedAt,_tmpDownloadedBytes,_tmpTotalBytes,_tmpPlaylistIndex,_tmpCarouselGroupId,_tmpCarouselIndex,_tmpCarouselTotal,_tmpIsDirectMediaUrl);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object nextQueued(final Continuation<? super DownloadRecord> $completion) {
    final String _sql = "SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY createdAt ASC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DownloadRecord>() {
      @Override
      @Nullable
      public DownloadRecord call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfForcedQuality = CursorUtil.getColumnIndexOrThrow(_cursor, "forcedQuality");
          final int _cursorIndexOfMediaType = CursorUtil.getColumnIndexOrThrow(_cursor, "mediaType");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfSpeedText = CursorUtil.getColumnIndexOrThrow(_cursor, "speedText");
          final int _cursorIndexOfEtaText = CursorUtil.getColumnIndexOrThrow(_cursor, "etaText");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfDownloadedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedBytes");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfPlaylistIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistIndex");
          final int _cursorIndexOfCarouselGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "carouselGroupId");
          final int _cursorIndexOfCarouselIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "carouselIndex");
          final int _cursorIndexOfCarouselTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "carouselTotal");
          final int _cursorIndexOfIsDirectMediaUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirectMediaUrl");
          final DownloadRecord _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpForcedQuality;
            if (_cursor.isNull(_cursorIndexOfForcedQuality)) {
              _tmpForcedQuality = null;
            } else {
              _tmpForcedQuality = _cursor.getString(_cursorIndexOfForcedQuality);
            }
            final String _tmpMediaType;
            if (_cursor.isNull(_cursorIndexOfMediaType)) {
              _tmpMediaType = null;
            } else {
              _tmpMediaType = _cursor.getString(_cursorIndexOfMediaType);
            }
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final int _tmpProgress;
            _tmpProgress = _cursor.getInt(_cursorIndexOfProgress);
            final String _tmpSpeedText;
            _tmpSpeedText = _cursor.getString(_cursorIndexOfSpeedText);
            final String _tmpEtaText;
            _tmpEtaText = _cursor.getString(_cursorIndexOfEtaText);
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpDownloadedBytes;
            _tmpDownloadedBytes = _cursor.getLong(_cursorIndexOfDownloadedBytes);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final Integer _tmpPlaylistIndex;
            if (_cursor.isNull(_cursorIndexOfPlaylistIndex)) {
              _tmpPlaylistIndex = null;
            } else {
              _tmpPlaylistIndex = _cursor.getInt(_cursorIndexOfPlaylistIndex);
            }
            final Long _tmpCarouselGroupId;
            if (_cursor.isNull(_cursorIndexOfCarouselGroupId)) {
              _tmpCarouselGroupId = null;
            } else {
              _tmpCarouselGroupId = _cursor.getLong(_cursorIndexOfCarouselGroupId);
            }
            final Integer _tmpCarouselIndex;
            if (_cursor.isNull(_cursorIndexOfCarouselIndex)) {
              _tmpCarouselIndex = null;
            } else {
              _tmpCarouselIndex = _cursor.getInt(_cursorIndexOfCarouselIndex);
            }
            final Integer _tmpCarouselTotal;
            if (_cursor.isNull(_cursorIndexOfCarouselTotal)) {
              _tmpCarouselTotal = null;
            } else {
              _tmpCarouselTotal = _cursor.getInt(_cursorIndexOfCarouselTotal);
            }
            final boolean _tmpIsDirectMediaUrl;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirectMediaUrl);
            _tmpIsDirectMediaUrl = _tmp != 0;
            _result = new DownloadRecord(_tmpId,_tmpUrl,_tmpForcedQuality,_tmpMediaType,_tmpTitle,_tmpStatus,_tmpProgress,_tmpSpeedText,_tmpEtaText,_tmpFilePath,_tmpThumbnailUrl,_tmpErrorMessage,_tmpCreatedAt,_tmpDownloadedBytes,_tmpTotalBytes,_tmpPlaylistIndex,_tmpCarouselGroupId,_tmpCarouselIndex,_tmpCarouselTotal,_tmpIsDirectMediaUrl);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
