package gapp.gui.util;

import java.util.Random;

public class NameGenerator 
{
    final private static Random randomizer = new Random();
    final private static String[] NAMELIST = 
        {
         "Abaco", 
         "Abbondanzio", 
         "Abbondio", 
         "Abdone", 
         "Abelardo", 
         "Abele", 
         "Abenzio", 
         "Abibo", 
         "Abramio", 
         "Abramo", 
         "Acacio", 
         "Acario", 
         "Accursio", 
         "Achille", 
         "Acilio", 
         "Aciscolo", 
         "Acrisio", 
         "Adalardo", 
         "Adalberto", 
         "Adalfredo", 
         "Adalgiso", 
         "Adalrico", 
         "Adamo", 
         "Addo", 
         "Adelardo", 
         "Adelberto", 
         "Adelchi", 
         "Adelfo", 
         "Adelgardo", 
         "Adelmo", 
         "Adeodato", 
         "Adolfo", 
         "Adone", 
         "Adrianov",
         "Adrione", 
         "Afro", 
         "Agabiov",
         "Agamennone", 
         "Agape", 
         "Agapito", 
         "Agazio", 
         "Agenore", 
         "Agesilao", 
         "Agostino", 
         "Agrippa", 
         "Aiace", 
         "Aidano", 
         "Aimone", 
         "Aladino", 
         "Alamanno", 
         "Alano", 
         "Alarico", 
         "Albano", 
         "Alberico", 
         "Alberto", 
         "Albino", 
         "Alboino", 
         "Albrico", 
         "Alceo", 
         "Alceste", 
         "Alcibiade", 
         "Alcide", 
         "Alcino", 
         "Aldo", 
         "Aldobrando", 
         "Aleandro", 
         "Aleardo", 
         "Aleramo", 
         "Alessandro", 
         "Alessio", 
         "Alfio", 
         "Alfonso", 
         "Alfredo", 
         "Algiso", 
         "Alighiero", 
         "Almerigo", 
         "Almiro", 
         "Aloisio", 
         "Alvaro", 
         "Alviero", 
         "Alvise", 
         "Amabile", 
         "Amadeo", 
         "Amando", 
         "Amanzio", 
         "Amaranto", 
         "Amato", 
         "Amatore", 
         "Amauri", 
         "Ambrogio", 
         "Ambrosiano", 
         "Amedeo", 
         "Amelio", 
         "Amerigo", 
         "Amico", 
         "Amilcare", 
         "Amintore", 
         "Amleto", 
         "Amone", 
         "Amore", 
         "Amos", 
         "Ampelio", 
         "Anacleto", 
         "Andrea", 
         "Angelo", 
         "Aniceto", 
         "Aniello", 
         "Annibale", 
         "Ansaldo", 
         "Anselmo", 
         "Ansovino", 
         "Antelmo", 
         "Antero", 
         "Antimo", 
         "Antino", 
         "Antioco", 
         "Antonello", 
         "Antonio", 
         "Apollinare", 
         "Apollo", 
         "Apuleio", 
         "Aquilino", 
         "Araldo", 
         "Aratone", 
         "Arcadio", 
         "Archimede", 
         "Archippo", 
         "Arcibaldo", 
         "Ardito", 
         "Arduino", 
         "Aresio", 
         "Argimiro", 
         "Argo", 
         "Arialdo", 
         "Ariberto", 
         "Ariele", 
         "Ariosto", 
         "Aris", 
         "Aristarco", 
         "Aristeo", 
         "Aristide", 
         "Aristione", 
         "Aristo", 
         "Aristofane", 
         "Aristotele", 
         "Armando", 
         "Arminio", 
         "Arnaldo", 
         "Aronne", 
         "Arrigo", 
         "Arturo", 
         "Ascanio", 
         "Asdrubale", 
         "Asimodeo", 
         "Assunto", 
         "Asterio", 
         "Astianatte", 
         "Ataleo", 
         "Atanasio", 
         "Athos", 
         "Attila", 
         "Attilano", 
         "Attilio", 
         "Auberto", 
         "Audace", 
         "Augusto", 
         "Aureliano", 
         "Aurelio", 
         "Auro", 
         "Ausilio", 
         "Averardo", 
         "Azeglio", 
         "Azelio",   
         "Abbondanza", 
         "Acilia", 
         "Ada",  
         "Adalberta", 
         "Adalgisa",     
         "Addolorata", 
         "Adelaide", 
         "Adelasia", 
         "Adele", 
         "Adelina", 
         "Adina",    
         "Adria", 
         "Adriana",  
         "Agata", 
         "Agnese", 
         "Agostina", 
         "Aida", 
         "Alba", 
         "Alberta", 
         "Albina", 
         "Alcina", 
         "Alda", 
         "Alessandra", 
         "Alessia", 
         "Alfonsa", 
         "Alfreda", 
         "Alice",  
         "Alida", 
         "Alina", 
         "Allegra", 
         "Alma",     
         "Altea", 
         "Amalia", 
         "Amanda", 
         "Amata",    
         "Ambra", 
         "Amelia",   
         "Amina", 
         "Anastasia", 
         "Anatolia", 
         "Ancilla",  
         "Andromeda", 
         "Angela", 
         "Angelica", 
         "Anita",    
         "Anna", 
         "Annabella", 
         "Annagrazia", 
         "Annamaria", 
         "Annunziata", 
         "Antea", 
         "Antonella", 
         "Antonia", 
         "Apollina", 
         "Apollonia", 
         "Appia", 
         "Arabella", 
         "Argelia", 
         "Arianna", 
         "Armida", 
         "Artemisa", 
         "Asella", 
         "Asia", 
         "Assunta", 
         "Astrid",   
         "Atanasia", 
         "Aurelia",  
         "Aurora", 
         "Ausilia",  
         "Ausiliatrice", 
         "Azelia",   
         "Azzurra"
        };
 
    public static String getRandomName()
    {
        return NAMELIST[randomizer.nextInt(NAMELIST.length)];                
    }
}
