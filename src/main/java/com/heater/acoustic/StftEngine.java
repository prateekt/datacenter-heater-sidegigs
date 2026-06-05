package com.heater.acoustic;

/**
 * Hann-window STFT and overlap-add iSTFT for MDMG v2 and Java-LDM.
 */
public final class StftEngine {

    public static final int DEFAULT_FFT_SIZE = 512;
    public static final int DEFAULT_HOP = 256;

    public record StftResult(double[][] logMagnitudes, double[][] phases, int sampleRateHz) {
        public int frameCount() {
            return logMagnitudes.length;
        }

        public int binCount() {
            return logMagnitudes.length > 0 ? logMagnitudes[0].length : 0;
        }
    }

    private StftEngine() {}

    public static StftResult forward(double[] samples, int sampleRateHz) {
        return forward(samples, sampleRateHz, DEFAULT_FFT_SIZE, DEFAULT_HOP);
    }

    public static StftResult forward(double[] samples, int sampleRateHz, int fftSize, int hop) {
        int bins = fftSize / 2 + 1;
        int frames = Math.max(1, 1 + (samples.length - fftSize) / hop);
        double[][] logMag = new double[frames][bins];
        double[][] phase = new double[frames][bins];
        double[] window = hann(fftSize);

        for (int f = 0; f < frames; f++) {
            int start = f * hop;
            double[] frame = new double[fftSize];
            for (int i = 0; i < fftSize; i++) {
                int idx = start + i;
                frame[i] = idx < samples.length ? samples[idx] * window[i] : 0.0;
            }
            double[] re = new double[bins];
            double[] im = new double[bins];
            fft(frame, re, im);
            for (int b = 0; b < bins; b++) {
                double mag = Math.hypot(re[b], im[b]);
                logMag[f][b] = Math.log10(Math.max(1e-10, mag));
                phase[f][b] = Math.atan2(im[b], re[b]);
            }
        }
        return new StftResult(logMag, phase, sampleRateHz);
    }

    public static double[] inverse(StftResult stft, int outputLength) {
        return inverse(stft, outputLength, DEFAULT_FFT_SIZE, DEFAULT_HOP);
    }

    public static double[] inverse(StftResult stft, int outputLength, int fftSize, int hop) {
        int bins = fftSize / 2 + 1;
        double[] window = hann(fftSize);
        double[] out = new double[outputLength];
        double[] norm = new double[outputLength];

        for (int f = 0; f < stft.frameCount(); f++) {
            double[] re = new double[bins];
            double[] im = new double[bins];
            for (int b = 0; b < bins; b++) {
                double mag = Math.pow(10.0, stft.logMagnitudes[f][b]);
                re[b] = mag * Math.cos(stft.phases()[f][b]);
                im[b] = mag * Math.sin(stft.phases()[f][b]);
            }
            double[] frame = ifft(re, im, fftSize);
            int start = f * hop;
            for (int i = 0; i < fftSize && start + i < outputLength; i++) {
                out[start + i] += frame[i] * window[i];
                norm[start + i] += window[i] * window[i];
            }
        }
        for (int i = 0; i < out.length; i++) {
            if (norm[i] > 1e-12) out[i] /= norm[i];
        }
        return out;
    }

    public static double[] flattenLogMag(StftResult stft) {
        int frames = stft.frameCount();
        int bins = stft.binCount();
        double[] flat = new double[frames * bins];
        int k = 0;
        for (int f = 0; f < frames; f++) {
            System.arraycopy(stft.logMagnitudes[f], 0, flat, k, bins);
            k += bins;
        }
        return flat;
    }

    public static StftResult unflattenLogMag(double[] flat, StftResult template) {
        int frames = template.frameCount();
        int bins = template.binCount();
        double[][] logMag = new double[frames][bins];
        int k = 0;
        for (int f = 0; f < frames; f++) {
            System.arraycopy(flat, k, logMag[f], 0, bins);
            k += bins;
        }
        return new StftResult(logMag, template.phases(), template.sampleRateHz());
    }

    private static double[] hann(int n) {
        double[] w = new double[n];
        for (int i = 0; i < n; i++) {
            w[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / Math.max(1, n - 1)));
        }
        return w;
    }

    /** Cooley-Tukey radix-2 FFT (real input). */
    static void fft(double[] input, double[] reOut, double[] imOut) {
        int n = input.length;
        double[] re = new double[n];
        double[] im = new double[n];
        System.arraycopy(input, 0, re, 0, n);
        bitReverse(re, im, n);
        for (int len = 2; len <= n; len <<= 1) {
            double angle = -2 * Math.PI / len;
            double wRe = Math.cos(angle);
            double wIm = Math.sin(angle);
            for (int i = 0; i < n; i += len) {
                double curRe = 1, curIm = 0;
                for (int j = 0; j < len / 2; j++) {
                    int a = i + j;
                    int b = i + j + len / 2;
                    double tRe = curRe * re[b] - curIm * im[b];
                    double tIm = curRe * im[b] + curIm * re[b];
                    re[b] = re[a] - tRe;
                    im[b] = im[a] - tIm;
                    re[a] += tRe;
                    im[a] += tIm;
                    double nextRe = curRe * wRe - curIm * wIm;
                    curIm = curRe * wIm + curIm * wRe;
                    curRe = nextRe;
                }
            }
        }
        int outBins = reOut.length;
        for (int k = 0; k < outBins; k++) {
            reOut[k] = re[k];
            imOut[k] = im[k];
        }
    }

    static double[] ifft(double[] reIn, double[] imIn, int n) {
        int bins = reIn.length;
        double[] re = new double[n];
        double[] im = new double[n];
        System.arraycopy(reIn, 0, re, 0, bins);
        System.arraycopy(imIn, 0, im, 0, bins);
        for (int k = bins; k < n; k++) {
            int j = n - k;
            re[k] = re[j];
            im[k] = -im[j];
        }
        bitReverse(re, im, n);
        for (int len = 2; len <= n; len <<= 1) {
            double angle = 2 * Math.PI / len;
            double wRe = Math.cos(angle);
            double wIm = Math.sin(angle);
            for (int i = 0; i < n; i += len) {
                double curRe = 1, curIm = 0;
                for (int j = 0; j < len / 2; j++) {
                    int a = i + j;
                    int b = i + j + len / 2;
                    double tRe = curRe * re[b] - curIm * im[b];
                    double tIm = curRe * im[b] + curIm * re[b];
                    re[b] = re[a] - tRe;
                    im[b] = im[a] - tIm;
                    re[a] += tRe;
                    im[a] += tIm;
                    double nextRe = curRe * wRe - curIm * wIm;
                    curIm = curRe * wIm + curIm * wRe;
                    curRe = nextRe;
                }
            }
        }
        double scale = 1.0 / n;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = re[i] * scale;
        return out;
    }

    private static void bitReverse(double[] re, double[] im, int n) {
        int j = 0;
        for (int i = 1; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                double tr = re[i]; re[i] = re[j]; re[j] = tr;
                double ti = im[i]; im[i] = im[j]; im[j] = ti;
            }
        }
    }
}
