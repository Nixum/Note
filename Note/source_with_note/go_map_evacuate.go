func growWork(t *maptype, h *hmap, bucket uintptr) {
    // make sure we evacuate the oldbucket corresponding
    // to the bucket we're about to use
    evacuate(t, h, bucket&h.oldbucketmask())

    // evacuate one more oldbucket to make progress on growing
    if h.growing() {
        evacuate(t, h, h.nevacuate)
    }
}

func evacuated(b *bmap) bool {
    h := b.tophash[0]
    return h > empty && h < minTopHash
}

func evacuate(t *maptype, h *hmap, oldbucket uintptr) {
    b := (*bmap)(add(h.oldbuckets, oldbucket*uintptr(t.bucketsize)))
    newbit := h.noldbuckets()
  // 判断是否搬移过
    if !evacuated(b) {
        // TODO: reuse overflow buckets instead of using new ones, if there
        // is no iterator using the old buckets.  (If !oldIterator.)

        // xy contains the x and y (low and high) evacuation destinations.
    // 吧bucket原先对应的索引赋值给x
        var xy [2]evacDst
        x := &xy[0]
        x.b = (*bmap)(add(h.buckets, oldbucket*uintptr(t.bucketsize)))
        x.k = add(unsafe.Pointer(x.b), dataOffset)
        x.v = add(x.k, bucketCnt*uintptr(t.keysize))
        // 如果是增量扩容，扩容后的bucket有变，假如以B=5为例，B+1= 6，这时候去倒数6位计算bucket的索引，但是倒数第6位只能是0或者1，也就是说索引只能是，x或y（x+newbit）,这里计算出来y，以备后用
        if !h.sameSizeGrow() {
            // Only calculate y pointers if we're growing bigger.
            // Otherwise GC can see bad pointers.
            y := &xy[1]
            y.b = (*bmap)(add(h.buckets, (oldbucket+newbit)*uintptr(t.bucketsize)))
            y.k = add(unsafe.Pointer(y.b), dataOffset)
            y.v = add(y.k, bucketCnt*uintptr(t.keysize))
        }
        // 进行搬移
        for ; b != nil; b = b.overflow(t) {
            k := add(unsafe.Pointer(b), dataOffset)
            v := add(k, bucketCnt*uintptr(t.keysize))
            for i := 0; i < bucketCnt; i, k, v = i+1, add(k, uintptr(t.keysize)), add(v, uintptr(t.valuesize)) {
                top := b.tophash[i]
        // 空的跳过
                if top == empty {
                    b.tophash[i] = evacuatedEmpty
                    continue
                }
                if top < minTopHash {
                    throw("bad map state")
                }
                k2 := k
                if t.indirectkey {
                    k2 = *((*unsafe.Pointer)(k2))
                }
                var useY uint8
                if !h.sameSizeGrow() {
                    // Compute hash to make our evacuation decision (whether we need
                    // to send this key/value to bucket x or bucket y).
          // 判断hash计算出来，是使用x还是y，等量扩容是使用x
                    hash := t.key.alg.hash(k2, uintptr(h.hash0))
                    if h.flags&iterator != 0 && !t.reflexivekey && !t.key.alg.equal(k2, k2) {
                        // If key != key (NaNs), then the hash could be (and probably
                        // will be) entirely different from the old hash. Moreover,
                        // it isn't reproducible. Reproducibility is required in the
                        // presence of iterators, as our evacuation decision must
                        // match whatever decision the iterator made.
                        // Fortunately, we have the freedom to send these keys either
                        // way. Also, tophash is meaningless for these kinds of keys.
                        // We let the low bit of tophash drive the evacuation decision.
                        // We recompute a new random tophash for the next level so
                        // these keys will get evenly distributed across all buckets
                        // after multiple grows.
                        useY = top & 1
                        top = tophash(hash)
                    } else {
                        if hash&newbit != 0 {
                            useY = 1
                        }
                    }
                }

                if evacuatedX+1 != evacuatedY {
                    throw("bad evacuatedN")
                }

                b.tophash[i] = evacuatedX + useY // evacuatedX + 1 == evacuatedY
                dst := &xy[useY]                 // evacuation destination
                // 如果目标的bucket已经满了，则新建overflow，挂载到bucket上，并使用这个overflow
                if dst.i == bucketCnt {
                    dst.b = h.newoverflow(t, dst.b)
                    dst.i = 0
                    dst.k = add(unsafe.Pointer(dst.b), dataOffset)
                    dst.v = add(dst.k, bucketCnt*uintptr(t.keysize))
                }
        // 拷贝key value，设置tophash数组的对应索引的值
                dst.b.tophash[dst.i&(bucketCnt-1)] = top // mask dst.i as an optimization, to avoid a bounds check
                if t.indirectkey {
                    *(*unsafe.Pointer)(dst.k) = k2 // copy pointer
                } else {
                    typedmemmove(t.key, dst.k, k) // copy value
                }
                if t.indirectvalue {
                    *(*unsafe.Pointer)(dst.v) = *(*unsafe.Pointer)(v)
                } else {
                    typedmemmove(t.elem, dst.v, v)
                }
                dst.i++
                // These updates might push these pointers past the end of the
                // key or value arrays.  That's ok, as we have the overflow pointer
                // at the end of the bucket to protect against pointing past the
                // end of the bucket.
                dst.k = add(dst.k, uintptr(t.keysize))
                dst.v = add(dst.v, uintptr(t.valuesize))
            }
        }
        // Unlink the overflow buckets & clear key/value to help GC.
        if h.flags&oldIterator == 0 && t.bucket.kind&kindNoPointers == 0 {
            b := add(h.oldbuckets, oldbucket*uintptr(t.bucketsize))
            // Preserve b.tophash because the evacuation
            // state is maintained there.
            ptr := add(b, dataOffset)
            n := uintptr(t.bucketsize) - dataOffset
            memclrHasPointers(ptr, n)
        }
    }

    if oldbucket == h.nevacuate {
        advanceEvacuationMark(h, t, newbit)
    }
}