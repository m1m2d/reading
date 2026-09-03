const CHUNK_SIZE = 5 * 1024 * 1024

export async function sha256File(file) {
  if (!window.crypto?.subtle) return null
  const buffer = await file.arrayBuffer()
  const hashBuffer = await window.crypto.subtle.digest('SHA-256', buffer)
  return Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

export async function uploadWithChunks(file, onProgress) {
  const totalChunks = Math.max(1, Math.ceil(file.size / CHUNK_SIZE))
  const { initChunk, uploadChunk, completeChunk } = await import('../api/books')
  const init = await initChunk({
    fileName: file.name,
    fileSize: file.size,
    totalChunks
  })
  let uploaded = 0
  for (let i = 0; i < totalChunks; i++) {
    const start = i * CHUNK_SIZE
    const chunk = file.slice(start, Math.min(start + CHUNK_SIZE, file.size))
    await uploadChunk(init.uploadId, i, chunk)
    uploaded += chunk.size
    onProgress && onProgress(Math.round((uploaded / file.size) * 100))
  }
  return completeChunk(init.uploadId, file.name)
}
