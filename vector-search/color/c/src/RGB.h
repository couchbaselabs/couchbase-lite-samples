//
//  RGB.cpp
//  color
//
//  Copyright (c) 2024 Couchbase, Inc All rights reserved.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

#pragma once

#include <sstream>
#include <string>
#include <vector>

class RGB {
public:
    /** Returns an RGB vector from hex color codes beginning with/without '#'
        or 3 space-separated integer numbers. */
    static std::vector<float> colorVector(std::string colorCode) {
        if (colorCode.empty()) {
            throw std::invalid_argument("Invalid color code");
        }

        int r = 0, g = 0, b = 0;
        if ((colorCode[0] == '#' && colorCode.size() == 7) || (colorCode[0] != '#' && colorCode.size() == 6)) {
            int pos = colorCode[0] == '#' ? 1 : 0;
            r = std::stoi(colorCode.substr(pos, 2), nullptr, 16);
            g = std::stoi(colorCode.substr(pos + 2, 2), nullptr, 16);
            b = std::stoi(colorCode.substr(pos + 4, 2), nullptr, 16);
        } else {
            auto comps = splitBySpace(colorCode);
            if (comps.size() != 3) {
                throw std::invalid_argument("Invalid color code");
            }
            r = std::stoi(comps[0]);
            g = std::stoi(comps[1]);
            b = std::stoi(comps[2]);
        }

        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            throw std::invalid_argument("Invalid color code");
        }

        std::vector<float> result{};
        result.push_back((float)r);
        result.push_back((float)g);
        result.push_back((float)b);
        return result;
    }
private:
    static std::vector<std::string> splitBySpace(std::string& str) {
        std::vector<std::string> components{};
        std::istringstream ss(str);
        std::string component;
        while (ss >> component) {
            components.push_back(component);
        }
        return components;
    }
};