package com.example.archnotes.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DAO のテストは SQLite が必要なので、端末/エミュレータで動く Instrumented Test にする。
 * インメモリ DB を使えばテスト間でデータが残らない。
 */
@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var database: NotesDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NotesDatabase::class.java).build()
        dao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndObserveAll_returnsNotesOrderedByUpdatedAtDesc() = runTest {
        dao.upsert(NoteEntity(title = "old", body = "", updatedAt = 1))
        dao.upsert(NoteEntity(title = "new", body = "", updatedAt = 2))

        val notes = dao.observeAll().first()

        assertEquals(listOf("new", "old"), notes.map { it.title })
    }

    @Test
    fun upsertWithExistingId_updatesRow() = runTest {
        val id = dao.upsert(NoteEntity(title = "before", body = "", updatedAt = 1))

        dao.upsert(NoteEntity(id = id, title = "after", body = "", updatedAt = 2))

        assertEquals("after", dao.observeById(id).first()?.title)
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun deleteById_removesRow() = runTest {
        val id = dao.upsert(NoteEntity(title = "x", body = "", updatedAt = 1))

        dao.deleteById(id)

        assertNull(dao.observeById(id).first())
    }
}
