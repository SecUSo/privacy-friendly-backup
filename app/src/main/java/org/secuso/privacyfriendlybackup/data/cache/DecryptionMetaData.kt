package org.secuso.privacyfriendlybackup.data.cache

import android.os.Parcel
import android.os.Parcelable
import org.openintents.openpgp.OpenPgpDecryptionResult
import org.openintents.openpgp.OpenPgpMetadata
import org.openintents.openpgp.OpenPgpSignatureResult

data class DecryptionMetaData(
    val signature: OpenPgpSignatureResult?,
    val decryption: OpenPgpDecryptionResult?,
    val metadata: OpenPgpMetadata?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        signature = (parcel.readParcelable<OpenPgpSignatureResult>(OpenPgpSignatureResult::class.java.classLoader)),
        decryption = (parcel.readParcelable<OpenPgpDecryptionResult>(OpenPgpDecryptionResult::class.java.classLoader)),
        metadata = (parcel.readParcelable<OpenPgpMetadata>(OpenPgpMetadata::class.java.classLoader))
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(signature, flags)
        parcel.writeParcelable(decryption, flags)
        parcel.writeParcelable(metadata, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DecryptionMetaData> {
        override fun createFromParcel(parcel: Parcel): DecryptionMetaData {
            return DecryptionMetaData(parcel)
        }

        override fun newArray(size: Int): Array<DecryptionMetaData?> {
            return arrayOfNulls(size)
        }
    }

}
