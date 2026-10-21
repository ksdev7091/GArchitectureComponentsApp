package com.example.archnotes.data.remote

import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ネットワークの代わりをする疑似リモートデータソース。
 *
 * 学習用に以下を再現している:
 * - 応答に時間がかかる(delay)
 * - 3 回に 1 回は失敗する(IOException)
 *
 * 本物の API クライアントに差し替えるときは、[NoteRemoteDataSource] を実装した
 * 別クラスを作り、DI モジュールのバインド先を変える。
 * 「失敗は IOException」という契約を守っていれば、Repository や ViewModel には手を入れずに済む。
 * これが「抽象に依存する」利点。
 */
@Singleton
class FakeNoteRemoteDataSource @Inject constructor() : NoteRemoteDataSource {

    private var callCount = 0

    override suspend fun fetchNotes(): List<NetworkNote> {
        delay(1_200) // 通信の遅延を模倣
        callCount++
        if (callCount % 3 == 0) {
            throw IOException("疑似ネットワークエラー (callCount=$callCount)")
        }
        val now = System.currentTimeMillis()
        // サーバー側 ID は文字列。ローカル DB の自動採番 ID とは別のカラムに保存されるので衝突しない
        return listOf(
            NetworkNote(
                id = "srv-1",
                title = "サーバーのメモ: 関心の分離とデータ駆動 UI",
                body = "1. 関心の分離 (Separation of concerns)\n2. データモデルから UI を駆動する (Drive UI from data models)",
                updatedAt = now - 3 * 60_000,
            ),
            NetworkNote(
                id = "srv-2",
                title = "サーバーのメモ: 単方向データフロー",
                body = "状態は上(ViewModel)から下(UI)へ流れ、イベントは下から上へ流れる。",
                updatedAt = now - 2 * 60_000,
            ),
            NetworkNote(
                id = "srv-3",
                title = "サーバーのメモ: 唯一の信頼できる情報源",
                body = "このアプリでは Room が Single Source of Truth。サーバーの応答は一度 Room に書き込んでから UI に届く。",
                updatedAt = now - 60_000,
            ),
        )
    }
}
