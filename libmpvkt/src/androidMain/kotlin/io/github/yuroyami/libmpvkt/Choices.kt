package io.github.yuroyami.libmpvkt

/*
 * One enum per mpv option that takes a fixed set of strings. Generated from the catalog in the
 * design notes and checked against mpv itself by MpvCatalogTest, which reads
 * option-info/<name>/choices on a device.
 */

public enum class AutoYesNo(override val mpvName: String) : MpvChoice {
    Auto("auto"),
    Yes("yes"),
    No("no"),
}

public enum class DeinterlaceMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Yes("yes"),
    Auto("auto"),
}

public enum class KeepOpenMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Yes("yes"),
    Always("always"),
}

public enum class IdleMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Yes("yes"),
    Once("once"),
}

public enum class HrSeekMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Yes("yes"),
    Absolute("absolute"),
    Default("default"),
}

public enum class FramedropMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Vo("vo"),
    Decoder("decoder"),
    DecoderPlusVo("decoder+vo"),
}

public enum class VideoSyncMode(override val mpvName: String) : MpvChoice {
    Audio("audio"),
    DisplayResample("display-resample"),
    DisplayResampleVdrop("display-resample-vdrop"),
    DisplayResampleDesync("display-resample-desync"),
    DisplayTempo("display-tempo"),
    DisplayVdrop("display-vdrop"),
    DisplayAdrop("display-adrop"),
    DisplayDesync("display-desync"),
    Desync("desync"),
}

public enum class Scaler(override val mpvName: String) : MpvChoice {
    Bilinear("bilinear"),
    BicubicFast("bicubic_fast"),
    Oversample("oversample"),
    Spline16("spline16"),
    Spline36("spline36"),
    Spline64("spline64"),
    Sinc("sinc"),
    Lanczos("lanczos"),
    EwaLanczos("ewa_lanczos"),
    EwaHanning("ewa_hanning"),
    EwaGinseng("ewa_ginseng"),
    EwaLanczossharp("ewa_lanczossharp"),
    EwaLanczos4sharpest("ewa_lanczos4sharpest"),
    EwaRobidoux("ewa_robidoux"),
    EwaRobidouxsharp("ewa_robidouxsharp"),
    Haasnsoft("haasnsoft"),
    Bicubic("bicubic"),
    Bcspline("bcspline"),
    CatmullRom("catmull_rom"),
    Mitchell("mitchell"),
    Hermite("hermite"),
    Gaussian("gaussian"),
    Nearest("nearest"),
    Box("box"),
    Triangle("triangle"),
}

public enum class TemporalScaler(override val mpvName: String) : MpvChoice {
    Oversample("oversample"),
    Linear("linear"),
    Spline16("spline16"),
    Spline36("spline36"),
    Spline64("spline64"),
    Sinc("sinc"),
    Lanczos("lanczos"),
    Ginseng("ginseng"),
    Bicubic("bicubic"),
    Hermite("hermite"),
    CatmullRom("catmull_rom"),
    Mitchell("mitchell"),
    Robidoux("robidoux"),
    Robidouxsharp("robidouxsharp"),
    Box("box"),
    Nearest("nearest"),
    Triangle("triangle"),
    Gaussian("gaussian"),
    Bartlett("bartlett"),
    Cosine("cosine"),
    Hanning("hanning"),
    Tukey("tukey"),
    Hamming("hamming"),
    Quadric("quadric"),
    Welch("welch"),
    Kaiser("kaiser"),
    Blackman("blackman"),
    Sphinx("sphinx"),
    Jinc("jinc"),
}

public enum class DitherMode(override val mpvName: String) : MpvChoice {
    Fruit("fruit"),
    Ordered("ordered"),
    ErrorDiffusion("error-diffusion"),
    No("no"),
}

public enum class ToneMapping(override val mpvName: String) : MpvChoice {
    Auto("auto"),
    Clip("clip"),
    Mobius("mobius"),
    Reinhard("reinhard"),
    Hable("hable"),
    Gamma("gamma"),
    Linear("linear"),
    Spline("spline"),
    Bt2390("bt.2390"),
    Bt2446a("bt.2446a"),
    St2094_40("st2094-40"),
    St2094_10("st2094-10"),
}

public enum class TransferCharacteristic(override val mpvName: String) : MpvChoice {
    Auto("auto"),
    Bt1886("bt.1886"),
    Srgb("srgb"),
    Linear("linear"),
    Gamma1_8("gamma1.8"),
    Gamma2_0("gamma2.0"),
    Gamma2_2("gamma2.2"),
    Gamma2_4("gamma2.4"),
    Gamma2_6("gamma2.6"),
    Gamma2_8("gamma2.8"),
    Prophoto("prophoto"),
    Pq("pq"),
    Hlg("hlg"),
    VLog("v-log"),
    SLog1("s-log1"),
    SLog2("s-log2"),
    St428("st428"),
}

public enum class ColorPrimaries(override val mpvName: String) : MpvChoice {
    Auto("auto"),
    Bt601_525("bt.601-525"),
    Bt601_625("bt.601-625"),
    Bt709("bt.709"),
    Bt2020("bt.2020"),
    Bt470m("bt.470m"),
    Apple("apple"),
    Adobe("adobe"),
    Prophoto("prophoto"),
    Cie1931("cie1931"),
    DciP3("dci-p3"),
    DisplayP3("display-p3"),
    VGamut("v-gamut"),
    SGamut("s-gamut"),
    Ebu3213("ebu3213"),
    FilmC("film-c"),
    AcesAp0("aces-ap0"),
    AcesAp1("aces-ap1"),
}

public enum class GpuApi(override val mpvName: String) : MpvChoice {
    Auto("auto"),
    Opengl("opengl"),
}

public enum class OpenglEsMode(override val mpvName: String) : MpvChoice {
    Auto("auto"),
    Yes("yes"),
    No("no"),
}

public enum class AspectMethod(override val mpvName: String) : MpvChoice {
    Bitstream("bitstream"),
    Container("container"),
}

public enum class VideoUnscaledMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Yes("yes"),
    DownscaleBig("downscale-big"),
}

public enum class ForceWindowMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Yes("yes"),
    Immediate("immediate"),
}

public enum class ScreenshotFormat(override val mpvName: String) : MpvChoice {
    Jpg("jpg"),
    Jpeg("jpeg"),
    Png("png"),
    Webp("webp"),
    Jxl("jxl"),
    Avif("avif"),
}

public enum class ScreenshotRawFormat(override val mpvName: String) : MpvChoice {
    Bgr0("bgr0"),
    Bgra("bgra"),
    Rgba("rgba"),
    Rgba64("rgba64"),
}

public enum class HorizontalAlign(override val mpvName: String) : MpvChoice {
    Left("left"),
    Center("center"),
    Right("right"),
}

public enum class VerticalAlign(override val mpvName: String) : MpvChoice {
    Top("top"),
    Center("center"),
    Bottom("bottom"),
}

public enum class Justify(override val mpvName: String) : MpvChoice {
    Auto("auto"),
    Left("left"),
    Center("center"),
    Right("right"),
}

public enum class SubAssOverrideMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Yes("yes"),
    Scale("scale"),
    Force("force"),
    Strip("strip"),
}

public enum class SubAssHinting(override val mpvName: String) : MpvChoice {
    None("none"),
    Light("light"),
    Normal("normal"),
    Native("native"),
}

public enum class SubAssShaper(override val mpvName: String) : MpvChoice {
    Simple("simple"),
    Complex("complex"),
}

public enum class SubAssUseVideoData(override val mpvName: String) : MpvChoice {
    None("none"),
    AspectRatio("aspect-ratio"),
    All("all"),
}

public enum class VsfilterColorCompat(override val mpvName: String) : MpvChoice {
    No("no"),
    Basic("basic"),
    Full("full"),
    Force601("force-601"),
}

public enum class AutoloadMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Exact("exact"),
    Fuzzy("fuzzy"),
    All("all"),
}

public enum class SubFontProvider(override val mpvName: String) : MpvChoice {
    Auto("auto"),
    None("none"),
    Fontconfig("fontconfig"),
}

public enum class BlendSubtitlesMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Yes("yes"),
    Video("video"),
}

public enum class OsdOnSeekMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Bar("bar"),
    Msg("msg"),
    MsgBar("msg-bar"),
}

public enum class GaplessAudioMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Yes("yes"),
    Weak("weak"),
}

public enum class ReplaygainMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Track("track"),
    Album("album"),
}

public enum class AudioDisplayMode(override val mpvName: String) : MpvChoice {
    No("no"),
    EmbeddedFirst("embedded-first"),
    ExternalFirst("external-first"),
}

public enum class ProbeInfoMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Yes("yes"),
    Auto("auto"),
    Nostreams("nostreams"),
}

public enum class SubtitlePrerollMode(override val mpvName: String) : MpvChoice {
    No("no"),
    Yes("yes"),
    Index("index"),
}

public enum class IndexMode(override val mpvName: String) : MpvChoice {
    Default("default"),
    Recreate("recreate"),
}

public enum class LoadFileMode(override val mpvName: String) : MpvChoice {
    Replace("replace"),
    Append("append"),
    AppendPlay("append-play"),
    InsertNext("insert-next"),
    InsertNextPlay("insert-next-play"),
    InsertAt("insert-at"),
    InsertAtPlay("insert-at-play"),
}

public enum class LoadListMode(override val mpvName: String) : MpvChoice {
    Replace("replace"),
    Append("append"),
    AppendPlay("append-play"),
    InsertNext("insert-next"),
    InsertNextPlay("insert-next-play"),
    InsertAt("insert-at"),
    InsertAtPlay("insert-at-play"),
}

public enum class SeekMode(override val mpvName: String) : MpvChoice {
    Relative("relative"),
    Absolute("absolute"),
    AbsolutePercent("absolute-percent"),
    RelativePercent("relative-percent"),
}

public enum class SeekPrecision(override val mpvName: String) : MpvChoice {
    Default(""),
    Keyframes("keyframes"),
    Exact("exact"),
}

public enum class RevertSeekMode(override val mpvName: String) : MpvChoice {
    Mark("mark"),
    MarkPermanent("mark-permanent"),
}

public enum class FrameStepMode(override val mpvName: String) : MpvChoice {
    Play("play"),
    Seek("seek"),
    Mute("mute"),
}

public enum class CycleDirection(override val mpvName: String) : MpvChoice {
    Up("up"),
    Down("down"),
}

public enum class ScreenshotMode(override val mpvName: String) : MpvChoice {
    Subtitles("subtitles"),
    Video("video"),
    Window("window"),
}

public enum class PlaylistNextMode(override val mpvName: String) : MpvChoice {
    Weak("weak"),
    Force("force"),
}

public enum class SubAddMode(override val mpvName: String) : MpvChoice {
    Select("select"),
    Auto("auto"),
    Cached("cached"),
}

public enum class SubtitleSlot(override val mpvName: String) : MpvChoice {
    Primary("primary"),
    Secondary("secondary"),
}

public enum class RescanMode(override val mpvName: String) : MpvChoice {
    Reselect("reselect"),
    KeepSelection("keep-selection"),
}

public enum class FilterOperation(override val mpvName: String) : MpvChoice {
    Set("set"),
    Add("add"),
    Toggle("toggle"),
    Remove("remove"),
    Clr("clr"),
    Pre("pre"),
    Help("help"),
}

public enum class ApplyProfileMode(override val mpvName: String) : MpvChoice {
    Apply("apply"),
    Restore("restore"),
}

public enum class ChangeListOp(override val mpvName: String) : MpvChoice {
    Set("set"),
    Append("append"),
    Add("add"),
    Pre("pre"),
    Clr("clr"),
    Remove("remove"),
    Toggle("toggle"),
}

public enum class OsdOverlayFormat(override val mpvName: String) : MpvChoice {
    AssEvents("ass-events"),
    None("none"),
}

public enum class TrackType(override val mpvName: String) : MpvChoice {
    Video("video"),
    Audio("audio"),
    Sub("sub"),
}
