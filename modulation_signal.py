import numpy as np
import matplotlib.pyplot as plt
import scipy.signal as signal
import wave
import datetime

class modulation_signal():
    def __init__(self) -> None:
        self.Nzc = 48  # ZC序列长度
        self.zc_length = 384  # 频谱数组长度
        self.fs = 48000  # 采样频率
        self.cutoff_freq = 21000

    def main(self):
        self.gen_audio()
        self.save_audio()
    
    def gen_audio(self, total_time = 60):
        # 生成一个复数ZC序列
        u = 1
        index = np.arange(self.Nzc)
        zc_sequence = np.exp(-1j * np.pi * u*index * (index + 1) / self.Nzc)

        # 执行DFT得到数据频谱数组di
        zc_fft = np.fft.fft(zc_sequence)

        # 将di并行映射到在不可听频率范围内的子载波上
        pad_length = self.zc_length  - self.Nzc# 补充0的数量
        spectrum_array = np.pad(zc_fft, (pad_length//2, pad_length - pad_length//2), mode='constant')

        # 执行IFFT得到调制信号，并提取实数信号
        zc_ifft = np.fft.ifft(spectrum_array)
        send_signal = list(np.real(zc_ifft))
        
        # 生成全部信号
        final_send_signal = list(send_signal) *int(total_time*self.fs / len(send_signal))
        final_send_signal = np.array(final_send_signal)

        # 滤波
        wn = self.cutoff_freq / (self.fs / 2)
        b, a = signal.butter(4, wn,btype='highpass', analog=False)
        self.final_send_signal = signal.lfilter(b, a, final_send_signal)

    def save_audio(self):    # 保存音频为.wav文件
        y = self.final_send_signal *10000    # 调节音量
        if np.max(y) > np.iinfo(np.int16).max:
            print("最大值超过 int16 的最大值", np.max(y), np.iinfo(np.int16).max)
            exit()
        # 判断最小值是否低于 int16 的最小值
        if np.min(y) < np.iinfo(np.int16).min:
            print("最小值低于 int16 的最小值", np.min(y))
            exit()
        audio_filename = "sending_audio.wav"
        y = np.array(y)
        f = wave.open(audio_filename, "wb")
        f.setnchannels(1)           # 配置声道数
        f.setsampwidth(2)           # 配置量化位数
        f.setframerate(self.fs)   # 配置取样频率
        f.writeframes(y.astype(np.short).tobytes())
        f.close()

tmp_obj = modulation_signal()
tmp_obj.main()