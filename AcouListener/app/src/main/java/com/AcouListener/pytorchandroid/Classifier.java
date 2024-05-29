package com.AcouListener.pytorchandroid;

import android.graphics.Bitmap;
import org.pytorch.Tensor;
import org.pytorch.Module;
import org.pytorch.IValue;
import org.pytorch.torchvision.TensorImageUtils;

//  用于分类的对象
public class Classifier {

    Module model;
    float[] mean = {0.485f, 0.456f, 0.406f};
    float[] std = {0.229f, 0.224f, 0.225f};
// 输入模型的文件地址
    public Classifier(String modelPath){
        model = Module.load(modelPath);
    }

    public void setMeanAndStd(float[] mean, float[] std){
        this.mean = mean;
        this.std = std;
    }
    // 将图像归一化处理
    public Tensor preprocess(Bitmap bitmap, int size){
        bitmap = Bitmap.createScaledBitmap(bitmap,size,size,false);
        return TensorImageUtils.bitmapToFloat32Tensor(bitmap,this.mean,this.std);
    }
    // 找到分类结果中的最大值
    public int argMax(float[] inputs){
        int maxIndex = -1;
        float maxvalue = 0.0f;
        for (int i = 0; i < inputs.length; i++){
            if(inputs[i] > maxvalue) {
                maxIndex = i;
                maxvalue = inputs[i];
            }

        }
        return maxIndex;
    }
    // 将bitmap图像处理成tensor，然后输入模型预测
    public String predict(Bitmap bitmap){
        Tensor tensor = preprocess(bitmap,224); // 归一化大小
        IValue inputs = IValue.from(tensor); // 转成tensor
        Tensor outputs = model.forward(inputs).toTensor(); // 预测
        float[] scores = outputs.getDataAsFloatArray();
        int classIndex = argMax(scores);  // 找到最大得分
        System.out.println("++++++++++++++++++++++++++++++++++++-------------");
        System.out.println( classIndex);
        String length = String.valueOf(scores.length);
        System.out.println("++++++++++++++++++++++++++++++" + length);

        return Constants.IMAGENET_CLASSES[classIndex];  // 将index转成名字

    }

}

