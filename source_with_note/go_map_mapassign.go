func mapassign(t *maptype, h *hmap, key unsafe.Pointer) unsafe.Pointer {
    if h == nil {
        panic(plainError("assignment to entry in nil map"))
    }
    // map并发读写的处理，直接抛异常
    if h.flags&hashWriting != 0 {
        throw("concurrent map writes")
    }
    // 根据map的hash种子 hash0，计算key的hash值
    alg := t.key.alg
    hash := alg.hash(key, uintptr(h.hash0))

    // Set hashWriting after calling alg.hash, since alg.hash may panic,
    // in which case we have not actually done a write.
    h.flags |= hashWriting
    // 如果map没有buckets，就分配（make(map)不指定map长度的时候就会惰性分配buckets）
    if h.buckets == nil {
        h.buckets = newobject(t.bucket) // newarray(t.bucket, 1)
    }

again:
    // 根据计算出的hash值，来确定应该插入的bucket在buckets中的索引
    bucket := hash & bucketMask(h.B)
    // 判断是否在扩容map，growWork是来完成扩容操作的
    if h.growing() {
        growWork(t, h, bucket)
    }
    // 确认bucket的地址
    b := (*bmap)(unsafe.Pointer(uintptr(h.buckets) + bucket*uintptr(t.bucketsize)))
    // 根据计算出hash二进制前八位的值，作为tophash使用
    top := tophash(hash)

    var inserti *uint8
    var insertk unsafe.Pointer
    var val unsafe.Pointer
    for {
        for i := uintptr(0); i < bucketCnt; i++ {
            // 循环遍历tophash数组，如果数组的索引位置为空，先拿过来使用
            if b.tophash[i] != top {
                if b.tophash[i] == empty && inserti == nil {
                    inserti = &b.tophash[i]
                    insertk = add(unsafe.Pointer(b), dataOffset+i*uintptr(t.keysize))
                    val = add(unsafe.Pointer(b), dataOffset+bucketCnt*uintptr(t.keysize)+i*uintptr(t.valuesize))
                }
                continue
            }
            // 找到了tophash数组中找到了当前key的tophash一致的情况
            k := add(unsafe.Pointer(b), dataOffset+i*uintptr(t.keysize))
            // 如果key是指针，获取指针对应的数据
            if t.indirectkey {
                k = *((*unsafe.Pointer)(k))
            }
            // 判断这两个key是否相同，不同继续寻找
            if !alg.equal(key, k) {
                continue
            }
            // already have a mapping for key. Update it.
            if t.needkeyupdate {
                typedmemmove(t.key, k, key)
            }
            // 根据i找到value应该存放的位置，可以结合结构图中bmap的数据结构来理解
            val = add(unsafe.Pointer(b), dataOffset+bucketCnt*uintptr(t.keysize)+i*uintptr(t.valuesize))
            goto done
        }
        // buckets中没有找到空余的位置或者相同的key，则到overflow中查找
        ovf := b.overflow(t)
        if ovf == nil {
            break
        }
        b = ovf
    }

    // Did not find mapping for key. Allocate new cell & add entry.

    // If we hit the max load factor or we have too many overflow buckets,
    // and we're not already in the middle of growing, start growing.
    // 判断是否需要扩容
    if !h.growing() && (overLoadFactor(h.count+1, h.B) || tooManyOverflowBuckets(h.noverflow, h.B)) {
        hashGrow(t, h)
        goto again // Growing the table invalidates everything, so try again
    }
    // inerti==nil，表示map的buckets都满了，则需要新加一个overflow挂载到map和对应的bmap下
    if inserti == nil {
        // all current buckets are full, allocate a new one.
        newb := h.newoverflow(t, b)
        inserti = &newb.tophash[0]
        insertk = add(unsafe.Pointer(newb), dataOffset)
        val = add(insertk, bucketCnt*uintptr(t.keysize))
    }

    // store new key/value at insert position
    // 存储key value到指定的位置
    if t.indirectkey {
        kmem := newobject(t.key)
        *(*unsafe.Pointer)(insertk) = kmem
        insertk = kmem
    }
    if t.indirectvalue {
        vmem := newobject(t.elem)
        *(*unsafe.Pointer)(val) = vmem
    }
    typedmemmove(t.key, insertk, key)
    *inserti = top
    h.count++

done:
    if h.flags&hashWriting == 0 {
        throw("concurrent map writes")
    }
    // 修改map的flags
    h.flags &^= hashWriting
    if t.indirectvalue {
        val = *((*unsafe.Pointer)(val))
    }
    return val
}