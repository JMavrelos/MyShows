package gr.blackswamp.myshows.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

class MediatorPairLiveData<S1, S2, O>(val source1: LiveData<S1>, val source2: LiveData<S2>, composer: (S1?, S2?) -> O) : MediatorLiveData<O>() {
    init {
        this.addSource(source1) {
            this.value = composer.invoke(it, source2.value)
        }
        this.addSource(source2) {
            this.value = composer.invoke(source1.value, it)
        }
    }
}