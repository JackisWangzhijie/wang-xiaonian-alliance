#!/usr/bin/env python3
import requests
import json
import time
import os

# Hugging Face Inference API for Stable Diffusion
API_URL = "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-xl-base-1.0"
headers = {"Authorization": f"Bearer hf_demo"}

def generate_image(prompt, filename):
    """Generate image using Hugging Face API"""
    payload = {
        "inputs": prompt,
        "parameters": {
            "width": 1024,
            "height": 1024,
            "guidance_scale": 7.5,
            "num_inference_steps": 50
        }
    }
    
    try:
        response = requests.post(API_URL, headers=headers, json=payload, timeout=120)
        if response.status_code == 200:
            with open(filename, 'wb') as f:
                f.write(response.content)
            print(f"✓ Generated: {filename}")
            return True
        else:
            print(f"✗ Failed {filename}: HTTP {response.status_code}")
            return False
    except Exception as e:
        print(f"✗ Error {filename}: {e}")
        return False

# Change to output directory
os.chdir('/root/.openclaw/workspace/小说/插图')

# Define all prompts
prompts = [
    ("Watercolor style illustration, warm healing tones. A young Chinese woman with medium wavy chestnut hair, gentle eyes, wearing cream blouse, pushing open wooden attic door in old Shanghai house, warm sunlight through skylight, cozy but slightly lonely atmosphere, soft lighting", "chapter-001.png"),
    
    ("Watercolor illustration, warm tones. Handsome Chinese man 35 years old, short black hair, mature elegant, standing behind coffee bar as barista, warm sunlight streaming through cafe window, romantic atmosphere", "chapter-006.png"),
    
    ("Watercolor style, warm lonely atmosphere. Young Chinese woman working late at night in small apartment, desk lamp casting silhouette, side profile view, cozy but solitary mood", "chapter-010.png"),
    
    ("Watercolor illustration, night scene. Young Chinese woman standing by riverside, city lights sparkling in background, thoughtful expression, warm healing tones, urban nightscape", "chapter-015.png"),
    
    ("Watercolor style, emotional scene. Young Chinese woman in hospital corridor, worried but strong expression, warm lighting, family concern atmosphere", "chapter-020.png"),
    
    ("Watercolor illustration, dramatic scene. Young Chinese woman in office, surprised expression looking at new boss, professional setting, warm tones", "chapter-021.png"),
    
    ("Watercolor romantic scene. Chinese couple standing together in Montmartre Paris, watching golden sunset, warm orange pink sky, romantic atmosphere, silhouettes", "chapter-025.png"),
    
    ("Watercolor celebration scene. Chinese couple toasting with wine glasses, celebrating success, warm joyful atmosphere, modern office or restaurant setting", "chapter-030.png"),
    
    ("Watercolor romantic proposal scene. Handsome Chinese man holding bouquet of flowers, sincere expression, romantic warm lighting, love confession moment", "chapter-031.png"),
    
    ("Watercolor romantic proposal. Chinese man kneeling on one knee with ring box, Eiffel Tower in background, golden hour lighting, romantic Paris scene", "chapter-035.png"),
]

# Generate all images
success_count = 0
for prompt, filename in prompts:
    print(f"Generating {filename}...")
    if generate_image(prompt, filename):
        success_count += 1
    time.sleep(2)  # Rate limiting

print(f"\nCompleted: {success_count}/{len(prompts)} images generated")
