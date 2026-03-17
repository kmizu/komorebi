/**
 * AudioWorklet processor: collects Float32 samples and posts chunks to the main thread.
 * Replaces the deprecated ScriptProcessorNode.
 */
const CHUNK_SIZE = 2048;

class PCMProcessor extends AudioWorkletProcessor {
  constructor() {
    super();
    this._buf = [];
  }

  process(inputs) {
    const ch = inputs[0]?.[0];
    if (!ch) return true;

    for (let i = 0; i < ch.length; i++) {
      this._buf.push(ch[i]);
      if (this._buf.length >= CHUNK_SIZE) {
        this.port.postMessage(new Float32Array(this._buf));
        this._buf = [];
      }
    }
    return true;
  }
}

registerProcessor('pcm-processor', PCMProcessor);
