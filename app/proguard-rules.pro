# R8 rules for NoteDeck's release build.
# Room, Hilt, WorkManager, Retrofit and kotlinx.serialization ship their own rules, so nothing here yet.
# Add -keep rules only for code R8 can't see being used, such as classes loaded by name.
