
import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
import scipy.signal as signal
import wave
from modulation_signal import modulation_signal


class demodulation_signal():
    def __init__(self) -> None:
        tmp_obj = modulation_signal()
        tmp_obj.gen_audio(384/48000)
        sending_signal = tmp_obj.final_send_signal    # 生成发射信号
        self.cutoff_freq = tmp_obj.cutoff_freq        # 截止频率，此处为21000
        self.standard_signal = self.demodulation_signal(sending_signal)    # 将发射信号下载波，生成做correlation的参考音频
        chunk = len(self.standard_signal)    # 参考音频的长度，此处为384
        # ------------------------------------------------------------------------------------------------------------

        # 读取音频
        filename = "sending_audio.wav"
        f = wave.open(filename, "rb")
        params = f.getparams()
        nchannels, sampwidth, framerate, nframes = params[:4]
        strData = f.readframes(nframes)
        waveData = np.frombuffer(strData, dtype=np.int16)  # 将字符串转化为int
        actual_nframes = waveData.shape[0]
        waveData = np.reshape(waveData, [actual_nframes, nchannels]).T
        waveData = waveData[0, :] 
        f.close()

        # 处理音频
        TMP_LIST = [4] # 处理信号的起始点
        colors = ['red', 'green','blue']
        n = 4  # 画图时展示的帧的个数
        for i in range(len(TMP_LIST)):
            start_index = TMP_LIST[i]*framerate
            received_signal = waveData[start_index: start_index + chunk*n]
            demoduled_signal = self.demodulation_signal(received_signal)    # 信号下转换
            Rn  = np.correlate(list(demoduled_signal), self.standard_signal)    # 计算相关函数
            n = n-1    # 算完相关性，会损失一帧
            Dn  = np.abs(Rn[chunk:chunk*(n)]) - np.abs(Rn[0:chunk*(n-1)])    # 计算R[n+N] - R[n]

            plt.plot(np.abs(Rn),color=colors[i])    # 画出Rn
            plt.plot(np.abs(Dn),color=colors[i+1])    # 画出Dn

        for i in range(len(Rn) // chunk):    # 画帧之间的分割线
            plt.axvline(x=i * chunk, color='gray', linestyle='--')
        plt.show()


    def demodulation_signal(self, received_signal):
        framerate = 48000
        t = np.linspace(0, len(received_signal)/framerate, len(received_signal))
        fc = self.cutoff_freq
        recieved_signal = np.exp(-1j * 2*np.pi * fc * t) * np.array(received_signal)

        # 滤波器参数
        cutoff_freq = (24000 - self.cutoff_freq)*2
        wn = cutoff_freq / (48000 / 2)
        b, a = signal.butter(4, wn, btype='lowpass', analog=False)
        recieved_signal = signal.lfilter(b, a, recieved_signal)

        return recieved_signal
    

tmp_obj = demodulation_signal()