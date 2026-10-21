# 演習問題(01-new-app: ArchNotes)

README の「読む順番」と「データの流れを追う」を終えてから取り組んでください。解答例は [answers.md](answers.md) にあります。
**解答を見る前に、必ず自分で書いて、ビルドしてテストを通してください。**

02 の演習が「同じ機能を各段階で作って差を見る」だったのに対し、こちらは
**完成形のアーキテクチャに機能を足していくとき、各層をどう通すか**を体に入れるための演習です。
難易度: ★ = 15 分程度、★★★ = 1 時間以上。

---

## 1 ★★ 設定を 1 つ増やす(Data 層 → Domain 層 → 2 つの画面)

設定画面に「文字サイズ(小 / 中 / 大)」を追加し、一覧画面と編集画面の両方の文字サイズに反映してください。条件:
- `UserPreferences` に `textScale`(enum)を追加し、`DataStoreUserPreferencesRepository` で保存・読み出しする。
- `SettingsScreen` にラジオボタンを追加する(既存の並び順と同じ作り)。
- 一覧画面と編集画面は、それぞれの UiState(または ViewModel)経由で値を受け取り、`MaterialTheme.typography` を拡大して描く。
- `FakeUserPreferencesRepository` を更新し、`SettingsViewModelTest` にテストを 1 つ追加する。

考えてほしいこと: 文字サイズは Domain 層(UseCase)を通すべきか。並び順が `GetNotesUseCase` を通っているのとは何が違うか。

## 2 ★★★ 「ピン留め」フィールドを追加する(Data 層からの一気通貫)

メモに「ピン留め」(Boolean)を追加し、一覧でピン留めを先頭に表示してください。条件:
- `NoteEntity` に `isPinned` カラムを追加し、DB の version を 2 に上げる。
  既存データを壊さないよう **Migration を書く**(`exportSchema = true` にして `AutoMigration` を使うのが簡単)。
- `Note`(モデル)、`NotesRepository`(`setPinned(id, pinned)`)、`NoteDao`、UI(行の端にピンのアイコン)を通す。
- `FakeNotesRepository`、`FakeNoteDao` も更新し、既存テストが全部通る状態を保つ。
- `OfflineFirstNotesRepositoryTest` に「同期してもローカルのピン留めは消えない」を追加する
  (ヒント: `saveNote` が `remoteId` を引き継いでいるのと同じ問題が `isPinned` にも起きる)。

考えてほしいこと: サーバーはピン留めを知らない。同期で上書きされないように、どこで守るか。

## 3 ★★ 同期失敗時に「再試行」できる Snackbar(イベントを状態で表す)

同期に失敗したときの Snackbar に「再試行」アクションを付け、押すと `refresh()` が再実行されるようにしてください。条件:
- `showSnackbar(message, actionLabel = ...)` の戻り値 `SnackbarResult.ActionPerformed` を使う。
- 「再試行を押した」というイベントは、UI → ViewModel のコールバック(`onRetry`)として流す。
- 状態なし版 `NotesScreen` のシグネチャを変える場合は Preview も直す。

考えてほしいこと: Snackbar のアクションが押されるまで ViewModel は何も知らない。これは UDF に反していますか。

## 4 ★★ MockWebServer のテストを増やす(ネットワーク層の契約)

`RetrofitNoteRemoteDataSourceTest` に次を追加してください。
- 空配列 `[]` が返ったら空リストになる
- 必須フィールド(`title`)が欠けた要素があると IOException になる
- サーバーが応答を返さない(タイムアウト)と IOException になる
  (ヒント: `MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)` と、OkHttpClient の `readTimeout` を短くしたクライアントを Retrofit に渡す)

考えてほしいこと: タイムアウトのテストで OkHttpClient をテスト側で組み立てることになる。本番の `NetworkModule` にタイムアウト設定がないのは問題か。

## 5 ★ 契約を破るとどうなるか見る

`RetrofitNoteRemoteDataSource` の `catch (e: HttpException)` を一時的に削除して `RetrofitNoteRemoteDataSourceTest` を実行してください。
どのテストが、どの例外で落ちますか。その例外が本番アプリで起きたら、`NotesViewModel.refresh()` はどうなりますか。
確認したら元に戻してください。

## 6 ★★★ E2E テストを 1 本増やす(Instrumented Test)

`src/androidTest` の `SyncFlowTest` は `TestRemoteModule` でリモートを差し替え、同期ボタン → 一覧にメモが出るまでを検証しています。
同じ仕組みで、次の流れを検証する E2E テストを書いてください。
- 追加ボタン → 編集画面でタイトルを入力 → 保存 → 一覧に表示される → 設定画面で「本文プレビュー」をオフ → 戻ると本文が消えている

考えてほしいこと:
- 設定の DataStore は本物が動くので、テスト間で設定が残る。`@Before` でどう初期化するか(ヒント: `PreferencesModule` も `@TestInstallIn` で差し替え、
  テストごとに別ファイル名にするか、`dataStore.edit { it.clear() }` を呼ぶ)。
- `SyncFlowTest` の `waitUntil` のような「非同期の完了待ち」をどこに置くか。
