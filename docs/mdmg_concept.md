# Mechanical Diffusion Model (MDM) — Concept

## The side-gig question

Data centers emit **structured noise** (fan BPF tones + broadband turbulence). Can that acoustic energy be **transformed** into something pleasant — even music — instead of pure nuisance?

This document defines the **Mechanical Diffusion Model (MDM)**: a physical analog of software latent diffusion, proposed as a speculative complement to rack-level metamaterial liners (MSE).

## Software diffusion (SOTA reference)

Modern music/audio generators (Stable Audio Open, AudioLDM 2, MusicGen) use **latent diffusion**:

1. **Encode** waveform → compact latent (VAE / EnCodec / STFT).
2. **Forward process** — add noise over T steps:  
   `x_t = sqrt(1-β_t) * x_{t-1} + sqrt(β_t) * ε`
3. **Learn** a score network ε_θ(x_t, t, prompt) to predict noise.
4. **Reverse** — iteratively denoise from x_T toward structured audio x_0.

Literature: Ho et al. (DDPM); Evans et al. (Stable Audio Open, 2024); Liu et al. (AudioLDM 2, 2023).

## Mechanical Diffusion Model (proposed hardware)

Replace the neural score with a **physical energy landscape** E_θ(x) encoded in coupled resonators:

```
dx = -∇E_θ(x) dt + σ(t) dW
```

where:

- **x** — modal amplitudes (STFT bins or mechanical normal modes)
- **E_θ(x) = ½ xᵀ K_θ x + V_template(x)** — spring network + nonlinear template potential
- **K_θ** — stiffness couplings (springs, membranes, Helmholtz cavities, waveguide segments)
- **σ(t)** — annealed physical noise (thermal / airflow turbulence)
- **Inference = physics** — no GPU matmuls at runtime

### Software ↔ mechanical mapping

| Software (SOTA LDM) | Mechanical (MDM) |
|---------------------|-------------------|
| VAE / EnCodec latent | STFT or modal decomposition |
| U-Net / DiT score net | Coupled resonator network |
| Backprop training | In-situ mechanical backprop (Nature Comm. 2024) or **distillation from Java-LDM** |
| DDPM scheduler | Physical noise annealing σ(t) |
| Text cross-attention | Prompt-conditioned pipe lengths / landscape regions |

### Literature anchors

- **Whitelam et al. (2024)** — Generative thermodynamic computing; Langevin denoising without digital U-Net.
- **Engheta et al. (2019)** — Wave physics as analog RNN; scattering media as temporal processors.
- **McMahon / Stein (2024)** — Physical neural networks; speaker + plate classification.
- **Mechanical NN (Nature Comm. 2024)** — In-situ backprop on 3D-printed spring networks.

## GPU fan orchestra (physical prior)

Inspired by the [Pickaso Rotary Bow](https://www.youtube.com/watch?v=N7B5sxk9OlA): each **GPU cooling fan** drives one **rotary-bow string cell** at Colossus-class scale (**15,000 fans** = 15,000 instruments in [`config/acoustic_spectrum.yaml`](../config/acoustic_spectrum.yaml)).

| Pickaso concept | Fan-orchestra mapping |
|-----------------|----------------------|
| Motorized elastic wheel | Fan shaft couples to bow wheel |
| Tremobow / Vase / Curved covers | Per-voice timbre preset (`bow_cover_cycle`) |
| Endless sustain | Continuous bowing while fans run |
| BPF blade clock | ~490 Hz hall-wide tremolo |

Implementation: `FanDrivenInstrumentField` (aggregate 1/3-octave SPL) + `BowedStringSynthesizer` (statistical multi-voice waveform). Java-LDM trains **fan noise → bowed-string field**; MDMG refines the **orchestra waveform**, not raw hum.

## This repo's Java-first demo pipeline

We do **not** port billion-parameter SOTA weights. Instead:

| Tier | Implementation | Role |
|------|----------------|------|
| MDMG v1 | `MechanicalDiffusionPhysics` | Teaching baseline (fixed template) |
| **Java-LDM** | `LatentDiffusionInference` + `ScoreNetwork` | Software GenAI ceiling — pure Java |
| **MDMG v2** | `MechanicalDiffusionPhysicsV2` | STFT Langevin with trainable K |
| **Distillation** | `MdmgLandscapeTrainer` | Fit K_θ from Java-LDM score trajectories |

Run:

```bash
./gradlew trainLatentDiffusion    # fit small Java score network
./gradlew trainMdmgLandscape      # distill springs from LDM
./gradlew generateMdmgBenchmark   # WAV clips + metrics + README
```

## Honest limits

- **True SOTA** (AudioLDM 2, Stable Audio) requires GPU-scale training — our Java-LDM is **task-specific** and small.
- **Mechanical hardware** at campus scale is **unbuilt**; distillation error is a feasibility screen, not a factory blueprint.
- Fan-noise→music is **not** a standard benchmark; we define a DC-side-gig task and compare tiers fairly.
- No Python at runtime; optional bundled reference WAVs cite literature SOTA without executing external models.
