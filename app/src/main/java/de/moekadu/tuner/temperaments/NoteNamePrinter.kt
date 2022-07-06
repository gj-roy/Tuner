package de.moekadu.tuner.temperaments

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import de.moekadu.tuner.R
import de.moekadu.tuner.views.SmallSuperScriptSpan

private val baseNoteResourceIds = mapOf(
    BaseNote.C to R.string.c_note_name,
    BaseNote.D to R.string.d_note_name,
    BaseNote.E to R.string.e_note_name,
    BaseNote.F to R.string.f_note_name,
    BaseNote.G to R.string.g_note_name,
    BaseNote.A to R.string.a_note_name,
    BaseNote.B to R.string.b_note_name,
)

private val modifierStrings = mapOf(
    NoteModifier.None to "",
    NoteModifier.Sharp to "\u266F",
    NoteModifier.Flat to "\u266D"
)

private val specialNoteNameResourceIds = mapOf(
    NoteNameStem(BaseNote.B, NoteModifier.Flat, BaseNote.A, NoteModifier.Sharp) to R.string.asharp_bflat_note_name,
    NoteNameStem(BaseNote.A, NoteModifier.Sharp, BaseNote.B, NoteModifier.Flat) to R.string.asharp_bflat_note_name
)

enum class MusicalNotePrintOptions { None, PreferFlat, PreferSharp }

/** Convenience note class storing the note base part (e.g. C#) and the octave as separate strings. */
data class MusicalNoteSubstrings(val note: String, val octaveIndex: Int) {
    val octave
        get() = if (octaveIndex == Int.MAX_VALUE) "" else octaveIndex.toString()
}

/** Create MusicalNoteSubstirngs from a musical note.
 * @param context Context for getting string resources.
 * @param printOption Options for printing (prefer flat/sharp ...)
 * @return class with note to print and octave index, while resolving the print options.
 */
fun MusicalNote.substrings(context: Context, printOption: MusicalNotePrintOptions = MusicalNotePrintOptions.None): MusicalNoteSubstrings {

    val specialNoteNameResourceId = specialNoteNameResourceIds[NoteNameStem.fromMusicalNote(this)]
    val specialNoteName = if (specialNoteNameResourceId == null)
        null
    else
        context.getString(specialNoteNameResourceId)

    val octaveIndex: Int

    // if the special note name is "-", it means that for the given translation there actually
    // is not special note name.
    val noteSubstring = if (specialNoteName != null && specialNoteName != "" && specialNoteName != "-") {
        octaveIndex = this.octave
        specialNoteName
    } else {
        val baseToPrint: BaseNote
        val modifierToPrint: NoteModifier
        if ((printOption == MusicalNotePrintOptions.PreferFlat && this.enharmonicBase != BaseNote.None && this.enharmonicModifier == NoteModifier.Flat)
            || (printOption == MusicalNotePrintOptions.PreferSharp && this.enharmonicBase != BaseNote.None && this.enharmonicModifier == NoteModifier.Sharp)) {
            baseToPrint = this.enharmonicBase
            modifierToPrint = this.enharmonicModifier
            octaveIndex = if (this.octave == Int.MAX_VALUE) Int.MAX_VALUE else this.octave + this.enharmonicOctaveOffset
        } else {
            baseToPrint = this.base
            modifierToPrint = this.modifier
            octaveIndex = this.octave
        }
        context.getString(baseNoteResourceIds[baseToPrint]!!) + modifierStrings[modifierToPrint]
    }
    return MusicalNoteSubstrings(noteSubstring, octaveIndex)
}

/** Create a char sequence which allows to print a note.
 * @param context Context for obtaining string resources
 * @param printOption Extra options for printing.
 * @param withOctave If true, the octave index will be printed, else it will be omitted.
 * @return SpannableString of the note.
 */
fun MusicalNote.toCharSequence(context: Context, printOption: MusicalNotePrintOptions = MusicalNotePrintOptions.None, withOctave: Boolean = true): CharSequence {
    val spannableStringBuilder = SpannableStringBuilder()

    val substrings = this.substrings(context, printOption)
    spannableStringBuilder.append(substrings.note)

    if (substrings.octaveIndex != Int.MAX_VALUE && withOctave) {
        spannableStringBuilder.append(
            SpannableString(substrings.octave).apply {
                setSpan(SmallSuperScriptSpan(),0, length,0)
            }
        )
    }
    return spannableStringBuilder
}
