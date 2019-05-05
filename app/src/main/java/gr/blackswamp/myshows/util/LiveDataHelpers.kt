package gr.blackswamp.myshows.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

class MediatorPairLiveData<S1, S2, O>(private val source1: LiveData<S1>, private val source2: LiveData<S2>, composer: (S1?, S2?) -> O) : MediatorLiveData<O>() {
    init {
        this.addSource(source1) {
            this.postValue(composer.invoke(it, source2.value))
        }
        this.addSource(source2) {
            this.postValue(composer.invoke(source1.value, it))
        }
    }
}

class MediatorTripleLiveData<S1, S2, S3, O>(private val source1: LiveData<S1>, private val source2: LiveData<S2>, private val source3: LiveData<S3>, composer: (S1?, S2?, S3?) -> O) : MediatorLiveData<O>() {
    init {
        this.addSource(source1) {
            this.value = composer.invoke(it, source2.value, source3.value)
        }
        this.addSource(source2) {
            this.value = composer.invoke(source1.value, it, source3.value)
        }
        this.addSource(source3) {
            this.postValue(composer.invoke(source1.value, source2.value, it))
        }
    }
}