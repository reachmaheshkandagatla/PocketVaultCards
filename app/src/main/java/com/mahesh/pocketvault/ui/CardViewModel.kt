package com.mahesh.pocketvault.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mahesh.pocketvault.data.AppDatabase
import com.mahesh.pocketvault.data.CardEntity
import com.mahesh.pocketvault.data.CardRepository
import com.mahesh.pocketvault.data.FolderEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CardViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val repo = CardRepository(db.cardDao(), db.folderDao())

    fun folders() = repo.folders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun cards(folderId: Long) = repo.cards(folderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun count(folderId: Long) = repo.count(folderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    fun deletedCards(folderId: Long) = repo.deletedCards(folderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun countDeleted(folderId: Long) = repo.countDeleted(folderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addFolder(folder: FolderEntity) = viewModelScope.launch { repo.addFolder(folder) }
    fun deleteFolder(folder: FolderEntity) = viewModelScope.launch { repo.deleteFolder(folder) }

    fun add(card: CardEntity) = viewModelScope.launch { repo.add(card) }
    fun markOpened(card: CardEntity) = viewModelScope.launch { repo.open(card) }
    fun togglePin(card: CardEntity) = viewModelScope.launch { repo.togglePin(card) }
    fun softDelete(card: CardEntity) = viewModelScope.launch { repo.softDelete(card) }
    fun restore(card: CardEntity) = viewModelScope.launch { repo.restore(card) }
    fun delete(card: CardEntity) = viewModelScope.launch { repo.delete(card) }
}
