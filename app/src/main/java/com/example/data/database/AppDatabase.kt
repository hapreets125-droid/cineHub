package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [MediaItemEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cinehub_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.mediaDao())
                    }
                }
            }
        }

        suspend fun populateDatabase(mediaDao: MediaDao) {
            val initialList = listOf(
                MediaItemEntity(
                    id = "study_group",
                    title = "Study Group",
                    type = "kdrama",
                    description = "Yoon Ga-min, un ragazzo con un talento naturale straordinario per le arti marziali ma che desidera ardentemente solo studiare, affronta i bulli peggiori del paese fondando un club di studio. Un'azione scolastica serrata e un dramma sul valore della perseveranza e dell'amicizia scolastica.",
                    imageUrl = "https://images.unsplash.com/photo-1546410531-bb4caa6b424d?w=500&auto=format&fit=crop&q=60",
                    releaseYear = "2025",
                    genre = "K-Drama, Azione, Scolastico",
                    rating = 4.9f,
                    platform = "Netflix",
                    duration = "16 Episodi",
                    cast = "Hwang Min-hyun, Han Ji-eun, Cha Woo-min",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                ),
                MediaItemEntity(
                    id = "dune_2",
                    title = "Dune - Parte Due",
                    type = "film",
                    description = "Arrakis è teatro di uno dei conflitti più spettacolari ed epici dell'universo. Paul Atreides si allea con Chani e la tribù Fremen per vendicarsi della distruzione dei suoi cari, lottando per prevenire una catastrofe intergalattica.",
                    imageUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&auto=format&fit=crop&q=60",
                    releaseYear = "2024",
                    genre = "Fantascienza, Epico, Azione",
                    rating = 4.8f,
                    platform = "Prime Video",
                    duration = "2h 46m",
                    cast = "Timothée Chalamet, Zendaya, Austin Butler",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                ),
                MediaItemEntity(
                    id = "inside_out_2",
                    title = "Inside Out 2",
                    type = "film",
                    description = "La centrale emotiva della mente di Riley riceve visite inaspettate! Gioia, Tristezza, Rabbia e gli altri devono convivere con la travolgente arrivata di Ansia, Noia, Invidia e Imbarazzo durante la crescita adolescenziale.",
                    imageUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500&auto=format&fit=crop&q=60",
                    releaseYear = "2024",
                    genre = "Animazione, Famiglia, Commedia",
                    rating = 4.7f,
                    platform = "Disney+",
                    duration = "1h 36m",
                    cast = "Kensington Tallman, Maya Hawke, Amy Poehler",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
                ),
                MediaItemEntity(
                    id = "squid_game_2",
                    title = "Squid Game - Stagione 2",
                    type = "serie",
                    description = "Il diabolico gioco per la sopravvivenza fa il suo spietato ritorno. Tre anni dopo la sua vittoria, il giocatore 456, Seong Gi-hun, rinuncia a partire per l'America ed entra nuovamente nell'arena con l'obiettivo di smascherare l'organizzazione.",
                    imageUrl = "https://images.unsplash.com/photo-1627856013091-fed6e4e30025?w=500&auto=format&fit=crop&q=60",
                    releaseYear = "2024",
                    genre = "Survival Thriller, Suspense",
                    rating = 4.9f,
                    platform = "Netflix",
                    duration = "9 Episodi",
                    cast = "Lee Jung-jae, Lee Byung-hun, Wi Ha-jun",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
                ),
                MediaItemEntity(
                    id = "fallout_s1",
                    title = "Fallout",
                    type = "serie",
                    description = "Tratta da un capolavoro dei videogiochi. 200 anni dopo l'apocalisse atomica, i fortunati superstiti delle lussuose cupole sotterranee Vault fanno ritorni nella spaventosa, desolata, radioattiva ma incredibilmente ironica superficie.",
                    imageUrl = "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=500&auto=format&fit=crop&q=60",
                    releaseYear = "2024",
                    genre = "Fantascienza, Azione, Grottesco",
                    rating = 4.6f,
                    platform = "Prime Video",
                    duration = "8 Episodi",
                    cast = "Ella Purnell, Aaron Moten, Walton Goggins",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                ),
                MediaItemEntity(
                    id = "my_demon",
                    title = "My Demon",
                    type = "kdrama",
                    description = "Un demone millenario perde improvvisamente tutti i suoi immensi poteri magici in un incidente stradale. L'unica soluzione è collaborare ed entrare in un finto matrimonio di convenienza con un'ereditiera gelida, scettica ed egocentrica.",
                    imageUrl = "https://images.unsplash.com/photo-1518156677180-95a2893f3e9f?w=500&auto=format&fit=crop&q=60",
                    releaseYear = "2024",
                    genre = "K-Drama, Fantasy, Romantico",
                    rating = 4.7f,
                    platform = "Netflix",
                    duration = "16 Episodi",
                    cast = "Kim Yoo-jung, Song Kang, Lee Sang-yi",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
                ),
                MediaItemEntity(
                    id = "the_bear",
                    title = "The Bear",
                    type = "serie",
                    description = "Carmy, brillante e perfezionista giovane stella della cucina gourmet internazionale di New York, torna alla sua città natale, Chicago, per rilevare la caotica panineria di carne lasciata dal fratello tragicamente scomparso.",
                    imageUrl = "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=500&auto=format&fit=crop&q=60",
                    releaseYear = "2024",
                    genre = "Drammatico, Spaccato di Vita",
                    rating = 4.8f,
                    platform = "Disney+",
                    duration = "10 Episodi",
                    cast = "Jeremy Allen White, Ebon Moss-Bachrach, Ayo Edebiri",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"
                ),
                MediaItemEntity(
                    id = "mercoledi_s1",
                    title = "Mercoledì",
                    type = "serie",
                    description = "La visionaria reinterpretazione di Tim Burton del leggendario personaggio di Mercoledì Addams. Tra indagini soprannaturali, strane alleanze scolastiche ed omicidi misteriosi nella magica Nevermore Academy.",
                    imageUrl = "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=500&auto=format&fit=crop&q=60",
                    releaseYear = "2022",
                    genre = "Giallo, Dark Fantasy, Commedia",
                    rating = 4.7f,
                    platform = "Netflix",
                    duration = "8 Episodi",
                    cast = "Jenna Ortega, Gwendoline Christie, Christina Ricci",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                )
            )
            mediaDao.insertMedia(initialList)
        }
    }
}
